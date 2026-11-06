# Platform engineer assessment

## Description

In this assessment you take a small app that only runs on a laptop, and make it run the way it would in production. You package it, deploy it on a Kubernetes cluster on your laptop, add monitoring and set up a pipeline. You also build a small nightly job that processes images and copes when things go wrong.

### What we give you

This repository has a small app with two parts:

- A Java (Spring Boot) API. You send it an area on a map, and it returns the satellite products in that area. The products are mock data.
- A web page where you log in, draw an area on a map and see the results.

Login uses Keycloak. Right now the app only runs on a developer's laptop. You start Keycloak with Docker Compose and the API with Maven. There's no Dockerfile, no pipeline, no Kubernetes setup and no monitoring.

### What you do

You take this app and run it the way we would run it for real:

- Package it in containers.
- Deploy it on a Kubernetes cluster on your laptop, together with Keycloak, a Postgres database and MinIO for file storage. Everything starts with one command.
- Add monitoring, so alerts go off when something breaks.
- Set up a GitHub Actions pipeline that tests the code and builds the containers.

You also build a small nightly job. It finds new images in MinIO, works out a few simple facts about each one (width, height and brightness) and saves them in Postgres. The job itself is simple. What matters is that it copes when things go wrong, like a broken image, MinIO being down, or the job being stopped halfway.

You don't add features to the app. The only code you change is for metrics and one test setting. The API has no database. Postgres is only for the nightly job.

### How we check it

We start your setup with one command. Then we break things on purpose: we stop parts of it, make the API fail and add a broken image. We check that the right alerts go off and that everything recovers. After that, we have a 30-minute call where you walk us through it.

## Run the app on your laptop

You need Java 17 or newer, and Docker.

1. Start Keycloak with `docker compose up -d`. It runs at http://localhost:8180. Wait 30 seconds for it to start.
2. Start the app with the command below. It runs at http://localhost:8080.
3. Open http://localhost:8080, log in, draw an area and click "Search products".

| Step | Mac or Linux | Windows |
|------|--------------|---------|
| Start the app | `./mvnw spring-boot:run` | `.\mvnw.cmd spring-boot:run` |
| Run the tests | `./mvnw test` | `.\mvnw.cmd test` |

On Mac or Linux, run `chmod +x mvnw` once first. The tests don't need Keycloak.

| Account | Username | Password |
|---------|----------|----------|
| App user | user | password |
| Keycloak admin | admin | admin |

The API has two endpoints:

- `GET /api/health` needs no login and returns "API is running".
- `POST /api/products/search` needs a login. Send a GeoJSON polygon and it returns the products that overlap it.

Example request body, an area around London. It returns one product, Sentinel-2A.

```json
{
  "type": "Polygon",
  "coordinates": [[
    [-0.5, 51.0], [0.5, 51.0], [0.5, 52.0],
    [-0.5, 52.0], [-0.5, 51.0]
  ]]
}
```

## Your tasks

The tasks are in order of importance.

### Task 1. Package the app

- Write Dockerfiles for the API and for the nightly job.
- Keep the container images small, run them as a normal user, and keep passwords out of them.

### Task 2. Run everything on a local cluster

- One command starts a kind or k3d cluster and deploys everything. One command removes it.
- The commands run in Git Bash on Windows, because that's what we test on.
- The start command builds the container images on our machine. It must not need a registry login.
- Deploy the API, Keycloak, Postgres, MinIO (S3-compatible storage), the nightly job and your monitoring.
- The official MinIO images are no longer published. Use another build, like `bitnamilegacy/minio`.
- Passwords and keys aren't in git or in the container images. Your start command can create them. The test user in `keycloak/realm-export.json` can stay.
- Every pod you write has CPU and memory limits. Long-running pods also have health checks. For charts you install, the defaults are fine.
- Postgres and MinIO keep their data when their pod restarts.
- When it's up, you can open the web page, log in through Keycloak and run a search.

### Task 3. Build the nightly job

**What it does**

- It's a Kubernetes CronJob that runs every night. It can also be started by hand.
- It looks for new images in the MinIO bucket `images`.
- For each new image, it saves the width, height and average brightness in the `image_results` table. Brightness is the average greyscale value, from 0 to 255.
- Write it in Python or Java.

**How it must behave**

- It's safe to run again. Images that are done are skipped and not downloaded again.
- If an image is replaced (it has a new ETag), it's analysed again.
- Two runs at the same time both finish without errors.
- If an image fails, the job tries it again a few times (you choose how many). Then it marks it as failed, with the reason, and carries on.
- A broken image isn't tried again, unless it's replaced.
- An image that failed because MinIO was down is tried again on the next run.
- A run that can't reach MinIO counts as a failed run.
- If a run is killed halfway, the next one carries on where it stopped.
- It writes one line per image to standard output: the image name, then done, skipped or failed. Failed lines include the reason.

**Setup**

- Create the table at deploy time, so nobody has to do it by hand.
- At deploy time, load the three `.jpg` files from the thumbnails folder into the bucket, so there's something to analyse. Leave out `placeholder.svg`. The folder is `src/main/resources/static/thumbnails/`.
- Our test images are PNG or JPEG files of up to 5 MB. A broken image is a file that isn't a real image.

```sql
CREATE TABLE image_results (
    image_key   text PRIMARY KEY,
    etag        text NOT NULL,
    status      text NOT NULL
                CHECK (status IN ('done', 'failed')),
    width       integer,
    height      integer,
    brightness  real,
    error       text,
    attempts    integer NOT NULL DEFAULT 1,
    updated_at  timestamptz NOT NULL DEFAULT now()
);
```

- `image_key` is the object name in the bucket. `etag` is its ETag.
- `attempts` is how many times this version of the image (this ETag) has been tried.
- You can add columns, tables or status values. Keep these columns and the primary key.

### Task 4. Add monitoring and alerts

- The API sends metrics to Prometheus. For the job, the metrics Kubernetes already gives you are enough.
- Alerts only need to reach Alertmanager. You don't need email or Slack.

You need at least these five alerts:

| Alert name | Fires when |
|------------|------------|
| ApiDown | The API isn't answering. |
| ApiErrors | Too many search requests fail. You choose the threshold. |
| KeycloakDown | Keycloak isn't answering. |
| JobFailed | A run of the nightly job failed. |
| JobOverdue | There's been no successful run for too long. You choose how long. |

- Each alert has a short description that says what's wrong.
- Put the label `team: assessment` on your own alerts only. Our test only reads alerts with this label, because some built-in alerts, like Watchdog, fire all the time.
- When everything is fine, none of your alerts fire.

**One code change**

Add an environment variable to the API called `FAULT_ERROR_RATE`. It's a number from 0 to 1, and it's 0 by default. At 0.5, half of the search requests fail with a 500 error. We use it to test ApiErrors.

### Task 5. Build the pipeline

- Use GitHub Actions.
- On every push and pull request: run the tests and build the container images.
- On a version tag, like `v0.1.0`: publish the container images. The GitHub registry is fine.
- Push one version tag before you send it, so we can see it work.

### Task 6. Write your README

Write your own `README.md` with:

- What to install, and the commands to start and stop everything.
- A simple diagram of the parts and how they talk to each other.
- The choices you made and why.
- What's missing or left out, and what you'd do with more time.
- How you'd run this on AWS, in one page at most.
- How long it took.

## How we rate it

Most important first:

1. It starts from your README with one command.
2. How it does in the break test.
3. The nightly job: how it handles failures, and how easy the code is to read.
4. The deployment: packaging, secrets, health checks, limits and data.
5. Monitoring: whether a person can tell what's wrong from your alerts and logs.
6. Safety: the app was written for a laptop, not for production. Did you spot what needs to change, and fix it or write it down?
7. The pipeline, your README and your commits.
