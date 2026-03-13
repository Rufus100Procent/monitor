# Monitor UI

A self-hosted web dashboard for monitoring Spring Boot applications in real-time.
Connects to a Monitor backend that polls your Spring Actuator endpoints and stores snapshots.
Displays live charts, historical data, server health, HTTP metrics, JVM stats, and more.


## Tech Stack

- **Vue 3** — Composition API with `<script setup>`
- **Vue Router 5**
- **TypeScript 5.9**
- **Vite 7.3** — build tool
- **chart.js 4.5** + **vue-chartjs 5.3** — charts
- **Lucide Vue Next** — icons
- **Scoped CSS with CSS variables** — no UI component library
- **Custom composables** — state management, no Pinia
- **Native fetch API** — no axios
- **Bun 1** — package manager and script runner
- **Docker multi-stage** — `oven/bun:1-alpine` for build, `nginxinc/nginx-unprivileged:alpine` for serve


## Prerequisites

- [Bun](https://bun.sh) installed locally for development
- [Docker](https://www.docker.com) for containerised builds
- **Monitor backend must be running on `http://localhost:8080`** before the frontend can make any API calls


## Run Locally (Development)

Install dependencies:

```sh
bun install
```

Start the dev server with hot-reload:

```sh
bun dev
```

App runs at `http://localhost:5173`.

> Make sure the Monitor backend is running on port `8080`, otherwise all API calls will fail.


## Other Commands

```sh
bun run type-check   # TypeScript type check only
bun run build        # Type-check + production bundle
bun run preview      # Preview the production build locally
bun run lint         # Run ESLint + oxlint
bun run format       # Format source files with Prettier
```


## Build and Run with Docker

Build the image:

```sh
docker build -t monitor-ui .
```

Run the container:

```sh
docker run -p 8173:8173 monitor-ui
```

App runs at `http://localhost:8173`.

> The Docker build runs a full type-check before bundling. TypeScript errors will fail the build.


## Screenshots

### Dashboard

![Dashboard](0.2.0-dashboard.png)

### Basics — Chart View

![Snapshot Chart View](snapshotchartview.png)

### Basics — Table View

![Snapshot Table View](snapshottableview.png)
