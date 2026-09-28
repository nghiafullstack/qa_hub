FROM node:22-alpine AS build
WORKDIR /build
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
# Bake sẵn base URL vào bundle — biến NEXT_PUBLIC_* chỉ đọc được lúc BUILD, không đọc lại lúc chạy.
ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
RUN npm run build

FROM node:22-alpine
WORKDIR /app
COPY --from=build /build/.next/standalone ./
COPY --from=build /build/.next/static ./.next/static
COPY --from=build /build/public ./public
EXPOSE 3000
ENTRYPOINT ["node", "server.js"]
