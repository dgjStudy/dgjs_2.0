FROM node:22-slim AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
# /etc/nginx/templates 의 *.template 은 컨테이너 시작 시 환경변수가 치환되어 conf.d 로 생성됨
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
ENV SWAGGER_ENABLED=false
EXPOSE 80
