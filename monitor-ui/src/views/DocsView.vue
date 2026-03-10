<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from 'lucide-vue-next'

const router = useRouter()
const activeSection = ref('')

const SECTIONS = [
  { id: 'what-is-actuator', label: 'What is Spring Actuator' },
  { id: 'getting-started',  label: 'Getting Started' },
  { id: 'inflation',        label: 'HTTP Inflation Problem' },
  { id: 'register',         label: 'Registering Your Server' },
  { id: 'metrics',          label: 'What Gets Monitored' },
]

function scrollTo(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function onScroll() {
  let current = ''
  for (const s of SECTIONS) {
    const el = document.getElementById(s.id)
    if (el && window.scrollY >= el.offsetTop - 110) current = s.id
  }
  activeSection.value = current
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})
onUnmounted(() => window.removeEventListener('scroll', onScroll))
</script>

<template>
  <div class="docs-shell">

    <!-- Top bar -->
    <header class="docs-bar">
      <button class="bar-brand" @click="router.push('/')">Monitor UI</button>
      <nav class="bar-actions">
        <button class="btn-docs-active" disabled>Docs</button>
      </nav>
    </header>

    <!-- Body -->
    <div class="docs-wrap">

      <!-- Sidebar TOC -->
      <aside class="toc-sidebar">
        <button class="back-btn" @click="router.back()">
          <ArrowLeft :size="15" />
          Back
        </button>
        <div class="toc-label">Contents</div>
        <ul class="toc-list">
          <li v-for="s in SECTIONS" :key="s.id">
            <button
              class="toc-link"
              :class="{ active: activeSection === s.id }"
              @click="scrollTo(s.id)"
            >{{ s.label }}</button>
          </li>
        </ul>
      </aside>

      <!-- Content -->
      <div class="content">

        <div class="content-header">
          <h1>Monitor UI Documentation</h1>
          <p>Monitor UI is tailored for Spring Boot Actuator. It makes it easy to visualize actuator metrics across all your running services.</p>
        </div>

        <!-- What is Spring Actuator -->
        <section id="what-is-actuator">
          <h2>What is Spring Actuator</h2>
          <p>Spring Boot Actuator is a built-in module that exposes the internals of your running application over HTTP. Without any extra code, it gives you endpoints that report on your application's health, memory consumption, CPU usage, active threads, HTTP request counts, garbage collection, and more.</p>
          <p>It comes bundled with Spring Boot and is activated with a single dependency. Once enabled, your application exposes a set of endpoints under <code>/actuator</code> that any monitoring tool, including Monitor UI, can read from.</p>
        </section>

        <!-- Getting Started -->
        <section id="getting-started">
          <h2>Getting Started with Monitor UI</h2>
          <p>Any Spring Boot application can be monitored as long as it has two things configured: the Actuator dependency and the correct properties to expose the endpoints.</p>

          <h3>Actuator Dependency</h3>
          <p>Add this to your <code>pom.xml</code>:</p>
          <pre><code>&lt;dependency&gt;
    &lt;groupId&gt;org.springframework.boot&lt;/groupId&gt;
    &lt;artifactId&gt;spring-boot-starter-actuator&lt;/artifactId&gt;
&lt;/dependency&gt;</code></pre>

          <p>Or if you are using Gradle, add this to your <code>build.gradle</code>:</p>
          <pre><code>implementation 'org.springframework.boot:spring-boot-starter-actuator'</code></pre>

          <h3>Application Properties</h3>
          <p>Add these two lines to your <code>application.properties</code> or the equivalent block in <code>application.yml</code>:</p>
          <pre><code>management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=always</code></pre>

          <p>The first line tells Spring to expose the <code>health</code>, <code>info</code>, and <code>metrics</code> endpoints over HTTP. By default Actuator keeps most endpoints closed. The second line tells Spring to include the full health breakdown in the response rather than just a single status word.</p>
        </section>

        <!-- HTTP Inflation Problem -->
        <section id="inflation">
          <h2>The HTTP Inflation Problem</h2>
          <p>The configuration above is enough to expose your metrics, but it comes with a side effect that will silently corrupt your data if left unfixed.</p>
          <p>The monitoring server contacts your <code>/actuator</code> endpoints on a regular interval to collect data. Spring tracks every HTTP request that reaches your application in a metric called <code>http.server.requests</code>. That tracking does not distinguish between a request from a real user and a request from your monitor. Every poll cycle gets counted.</p>

          <div class="note">
            <strong>Example:</strong> every poll cycle makes 14 requests to your app, 7 to collect JVM and system metrics, 7 to collect HTTP request data. At a 10-second poll interval that is 6 poll cycles per minute, which adds up to 84 requests per minute and 120,960 requests per day appearing in your metrics as real user traffic. Your request counts are inflated, your average response times are skewed, and your 2xx breakdown includes monitor traffic that has nothing to do with your users
          </div>

          <p>The result is a dashboard that looks accurate but is not. The fix below takes less than a minute to add.</p>

          <pre><code>@Bean
public ObservationPredicate excludeActuatorObservations() {
    return (name, context) -> {
        if (name.equals("http.server.requests")
                &amp;&amp; context instanceof ServerRequestObservationContext ctx) {
            assert ctx.getCarrier() != null;
            return !ctx.getCarrier().getRequestURI().startsWith("/actuator");
        }
        return true;
    };
}</code></pre>

          <h3>What is ObservationPredicate</h3>
          <p>Spring Boot 3 tracks activity through a system called Observations. Before Spring records any observation, it runs it through all registered <code>ObservationPredicate</code> beans. If the predicate returns <code>true</code>, the observation is recorded. If it returns <code>false</code>, it is dropped entirely — the request still completes, it simply never gets counted in your metrics.</p>
          <p>This bean says: if the observation is an HTTP request and the URL begins with <code>/actuator</code>, drop it. Everything else passes through unchanged. Your <code>http.server.requests</code> metric will only reflect real user traffic.</p>
        </section>

        <!-- Registering -->
        <section id="register">
          <h2>Registering Your Server</h2>
          <p>Once your application is configured, the final step is to register it inside Monitor UI. After logging in, open the dashboard and add a new server. You will be asked for the following:</p>

          <h3>Application Name</h3>
          <p>A label to identify this server in your dashboard. This can be anything meaningful to your team, such as the service name or environment.</p>

          <h3>Application Version</h3>
          <p>The version of the application you are registering. This helps you track which deployment you are looking at when reviewing metrics over time.</p>

          <h3>Base URL</h3>
          <p>The root URL of your running application, for example <code>http://your-server.com</code> or <code>http://localhost:8080</code>. Do not include the Actuator path here.</p>

          <h3>Actuator Path</h3>
          <p>The path where Actuator is mounted. This is <code>/actuator</code> by default unless you have changed it in your configuration.</p>

          <h3>Poll Interval</h3>
          <p>How frequently the monitoring server should contact your application to collect metrics. A lower interval gives more granular data. A higher interval reduces requests made to your app. Poll interval must be between 4 seconds to 60 seconds</p>

          <p>Once submitted, Monitor UI will combine the base URL and actuator path to validate that your server is reachable and responding. From that point forward, metrics are collected automatically on each poll cycle.</p>
        </section>

        <!-- What Gets Monitored -->
        <section id="metrics">
          <h2>What Gets Monitored</h2>
          <p>Once your server is registered, Monitor UI collects the following metrics on every poll cycle.</p>

          <div class="metric">
            <div class="metric-title">CPU Usage</div>
            <span class="metric-label">What it is</span>
            <p>The percentage of CPU being consumed by the JVM process at the time of the poll.</p>
            <span class="metric-label">Why it matters</span>
            <p>CPU that stays elevated over time is almost always a sign that something is wrong — a tight loop, an inefficient query, or threads competing for the same resource. Seeing this early gives you time to investigate before it degrades the user experience.</p>
          </div>

          <div class="metric">
            <div class="metric-title">System Load</div>
            <span class="metric-label">What it is</span>
            <p>The 1-minute load average of the operating system, covering all processes on the host, not only the JVM.</p>
            <span class="metric-label">Why it matters</span>
            <p>Your application may look healthy from the inside while the machine it runs on is under pressure from something else. A high system load with a normal JVM CPU reading tells you another process is consuming resources on the same host.</p>
          </div>

          <div class="metric">
            <div class="metric-title">JVM Threads</div>
            <span class="metric-label">What it is</span>
            <p>The number of threads currently alive inside the JVM, including active, sleeping, and blocked threads.</p>
            <span class="metric-label">Why it matters</span>
            <p>A thread count that rises steadily and never comes back down is one of the clearest signs of a thread leak. Threads are expensive — each one holds memory. Left unchecked, a leak will exhaust the JVM's ability to create new threads.</p>
          </div>

          <div class="metric">
            <div class="metric-title">GC Overhead</div>
            <span class="metric-label">What it is</span>
            <p>The percentage of total elapsed time the JVM is spending running garbage collection rather than executing your application code.</p>
            <span class="metric-label">Why it matters</span>
            <p>Garbage collection pauses your application. When GC overhead climbs above roughly 5%, you will typically see latency spikes as the JVM stops the world to reclaim memory. Sustained high overhead is usually the first symptom of a memory leak.</p>
          </div>

          <div class="metric">
            <div class="metric-title">Disk Space</div>
            <span class="metric-label">What it is</span>
            <p>The amount of disk space currently used compared to the total available on the volume where your application is running.</p>
            <span class="metric-label">Why it matters</span>
            <p>A full disk is one of the most disruptive failures a server can experience. Log writers stop, file uploads fail, and databases can corrupt their write-ahead logs. Monitoring disk usage gives you time to act before saturation.</p>
          </div>

          <div class="metric">
            <div class="metric-title">Uptime</div>
            <span class="metric-label">What it is</span>
            <p>The total amount of time the server has been running continuously since its last start.</p>
            <span class="metric-label">Why it matters</span>
            <p>A sudden drop in uptime means the application restarted. If that restart was not planned, it indicates a crash. If you see uptime resetting repeatedly, you are looking at a crash-restart loop.</p>
          </div>

          <div class="metric">
            <div class="metric-title">JVM Heap Memory</div>
            <span class="metric-label">What it is</span>
            <p>The amount of JVM heap memory currently in use after garbage collection has run.</p>
            <span class="metric-label">Why it matters</span>
            <p>Heap memory that increases with each GC cycle and never returns to its previous baseline is the textbook definition of a memory leak. Tracking this over time makes the pattern unmistakable long before the JVM throws an OutOfMemoryError.</p>
          </div>

          <div class="metric">
            <div class="metric-title">Total Requests</div>
            <span class="metric-label">What it is</span>
            <p>The count of all HTTP requests that reached your application during the most recent poll interval.</p>
            <span class="metric-label">Why it matters</span>
            <p>Request volume is your clearest signal of whether your application is reachable and being used normally. A sharp drop might mean your service is offline. A sudden spike can indicate a bot, a flood of retries, or a misconfigured client looping.</p>
          </div>

          <div class="metric">
            <div class="metric-title">HTTP Status Codes</div>
            <span class="metric-label">What it is</span>
            <p>A live breakdown of HTTP responses grouped by status class: 2xx success, 3xx redirects, 4xx client errors, and 5xx server errors.</p>
            <span class="metric-label">Why it matters</span>
            <p>A rising 5xx rate means your application is failing to handle requests. A rising 4xx rate typically points to broken authentication or a recent API change clients have not yet adapted to. Watching both in real time lets you react before users start reporting problems.</p>
          </div>

          <div class="metric">
            <div class="metric-title">Response Time</div>
            <span class="metric-label">What it is</span>
            <p>The average time in milliseconds your application takes to respond to HTTP requests, measured per poll cycle.</p>
            <span class="metric-label">Why it matters</span>
            <p>Response time is often the first signal that something is wrong, appearing before error rates rise or the application becomes fully unavailable. A slow database query, a saturated connection pool, or a thread bottleneck will all show up as increased latency first.</p>
          </div>
        </section>

      </div>
    </div>
  </div>
</template>

<style scoped>
/*  Shell  */
.docs-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
  color: var(--text-primary);
  font-size: 15px;
  line-height: 1.7;
  zoom: 1.15;
}

/*  Top bar */
.docs-bar {
  height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 20;
}

.bar-brand {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.01em;
  background: linear-gradient(135deg, hsl(270,70%,55%) 0%, hsl(300,65%,58%) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  cursor: pointer;
  transition: opacity 0.15s;
}
.bar-brand:hover { opacity: 0.8; }

.bar-actions { display: flex; align-items: center; gap: 6px; }

.btn-docs-active {
  padding: 6px 13px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  background: var(--nav-hover);
  color: var(--text-primary);
  border: 1px solid var(--border);
  cursor: default;
}

/*  Layout  */
.docs-wrap {
  display: flex;
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 32px;
  gap: 64px;
  width: 100%;
  box-sizing: border-box;
}

/*  Sidebar TOC  */
.toc-sidebar {
  width: 200px;
  flex-shrink: 0;
  padding: 48px 0;
  position: sticky;
  top: 58px;
  height: calc(100vh - 58px);
  overflow-y: auto;
}

.back-btn {
  background: none;
  font-size: 13px;
  cursor: pointer;
  color: var(--text-muted);
  padding: 0;
  margin-bottom: 28px;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: color 0.15s;
}
.back-btn:hover { color: var(--text-primary); }

.toc-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.toc-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.toc-link {
  display: block;
  width: 100%;
  text-align: left;
  font-size: 13px;
  color: var(--text-secondary);
  background: none;
  padding: 5px 10px;
  border-radius: 6px;
  border-left: 2px solid transparent;
  transition: all 0.15s;
  cursor: pointer;
  line-height: 1.4;
}

.toc-link:hover {
  color: var(--text-primary);
  background: var(--nav-hover);
}

.toc-link.active {
  color: var(--text-primary);
  border-left-color: var(--accent);
  background: var(--nav-hover);
  font-weight: 600;
}

/*  Content  */
.content {
  flex: 1;
  padding: 48px 0 96px;
  min-width: 0;
}

.content-header {
  margin-bottom: 52px;
  padding-bottom: 28px;
  border-bottom: 1px solid var(--border);
}

.content-header h1 {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.02em;
  margin-bottom: 10px;
  color: var(--text-primary);
}

.content-header p {
  font-size: 15px;
  color: var(--text-muted);
  max-width: 520px;
  line-height: 1.65;
}

/* Sections */
section {
  margin-bottom: 60px;
  scroll-margin-top: 88px;
}

section h2 {
  font-size: 19px;
  font-weight: 700;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border);
  color: var(--text-primary);
}

section h3 {
  font-size: 14px;
  font-weight: 600;
  margin-top: 24px;
  margin-bottom: 8px;
  color: var(--text-primary);
}

section p {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 12px;
  line-height: 1.75;
}

/* Code */
pre {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px 18px;
  font-size: 13px;
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  overflow-x: auto;
  line-height: 1.6;
  margin: 14px 0;
}

code {
  font-family: "SFMono-Regular", Consolas, "Liberation Mono", monospace;
  font-size: 12.5px;
  background: var(--surface);
  border: 1px solid var(--border);
  padding: 1px 5px;
  border-radius: 4px;
  color: hsl(162, 55%, 38%);
}

pre code {
  background: none;
  border: none;
  padding: 0;
  color: var(--text-primary);
  font-size: 13px;
}

/* Note callout */
.note {
  background: hsl(45, 90%, 97%);
  border-left: 3px solid hsl(38, 85%, 55%);
  padding: 12px 16px;
  margin: 16px 0;
  font-size: 13.5px;
  color: var(--text-secondary);
  border-radius: 0 6px 6px 0;
  line-height: 1.65;
}

.note strong { color: var(--text-primary); }

:root.dark .note {
  background: hsl(38, 40%, 12%);
  border-left-color: hsl(38, 70%, 45%);
}

/* Metric blocks */
.metric {
  border: 1.5px solid var(--border);
  border-radius: 10px;
  padding: 18px 20px;
  margin-bottom: 14px;
  transition: border-color 0.2s;
}

.metric:hover { border-color: hsl(270, 55%, 78%); }
:root.dark .metric:hover { border-color: hsl(270, 40%, 40%); }

.metric-title {
  font-size: 14px;
  font-weight: 700;
  margin-bottom: 10px;
  color: var(--text-primary);
}

.metric p { margin-bottom: 8px; }
.metric p:last-child { margin-bottom: 0; }

.metric-label {
  display: inline-block;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 4px;
}


/*  Responsive  */
@media (max-width: 768px) {
  .docs-wrap     { flex-direction: column; padding: 0 18px; gap: 0; }
  .toc-sidebar   { position: static; height: auto; width: 100%; padding: 28px 0 0; border-bottom: 1px solid var(--border); margin-bottom: 32px; }
  .docs-bar      { padding: 0 18px; }
  .content       { padding: 32px 0 64px; }
}
</style>
