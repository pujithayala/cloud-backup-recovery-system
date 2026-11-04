# Production Nginx container for Cloud Backup Frontend
FROM nginx:alpine

# Copy custom nginx reverse-proxy configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy frontend static assets (zero build step needed)
COPY index.html /usr/share/nginx/html/
COPY app.js /usr/share/nginx/html/

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

