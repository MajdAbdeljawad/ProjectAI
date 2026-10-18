import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import express from 'express';
import http from 'node:http';
import { join } from 'node:path';

const browserDistFolder = join(import.meta.dirname, '../browser');

const app = express();
const angularApp = new AngularNodeAppEngine();

try {
  process.loadEnvFile();
} catch {
  // .env file not present or already loaded
}

const SPRING_HOST = process.env['SPRING_HOST'] || 'localhost';
const SPRING_PORT = Number(process.env['SPRING_PORT'] || 8080);

function proxyToSpring(req: express.Request, res: express.Response): void {
  const incoming = req.originalUrl || req.url;
  const path = incoming.startsWith('/api') ? incoming : `/api${incoming}`;
  const headers = { ...req.headers, host: `${SPRING_HOST}:${SPRING_PORT}` };
  delete headers['content-length'];

  const proxyReq = http.request(
    {
      hostname: SPRING_HOST,
      port: SPRING_PORT,
      path,
      method: req.method,
      headers,
    },
    (proxyRes) => {
      res.writeHead(proxyRes.statusCode ?? 502, proxyRes.headers);
      proxyRes.pipe(res);
    },
  );

  proxyReq.on('error', () => {
    if (!res.headersSent) {
      res.status(502).json({
        error: `Backend Spring Boot injoignable sur ${SPRING_HOST}:${SPRING_PORT}. Démarrez backend_meal.`,
      });
    }
  });

  req.pipe(proxyReq);
}

app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, PATCH, DELETE, OPTIONS');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization, x-user-id');
  if (req.method === 'OPTIONS') {
    res.sendStatus(200);
    return;
  }
  next();
});

// Forward nutrition API to Spring Boot (must run before express.json so the body is streamed)
app.use('/nutrition', proxyToSpring);
app.use('/api/nutrition', proxyToSpring);

app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

/**
 * Handle all other requests by rendering the Angular application.
 */
app.use((req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      response ? writeResponseToNodeResponse(response, res) : next(),
    )
    .catch(next);
});

if (isMainModule(import.meta.url) || process.env['pm_id']) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, (error) => {
    if (error) {
      throw error;
    }

    console.log(`Node Express server listening on http://localhost:${port}`);
    console.log(`Nutrition API proxied to Spring Boot http://${SPRING_HOST}:${SPRING_PORT}`);
  });
}

export const reqHandler = createNodeRequestHandler(app);
