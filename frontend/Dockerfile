# ---- Build stage: install dependencies and build the static React app ----
FROM node:22-alpine AS build
WORKDIR /app

# Install dependencies first. This layer is cached while package files do not change.
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- Runtime stage: Nginx serves the built files and proxies /api to the Payment Router ----
FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
