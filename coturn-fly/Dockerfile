FROM coturn/coturn:latest

# Copy configuration file
COPY turnserver.conf /etc/turnserver.conf

# Expose required ports
EXPOSE 3478/tcp 3478/udp 5349/tcp 5349/udp
EXPOSE 49152-49200/udp

# Run turnserver with specified configuration
CMD ["turnserver", "-c", "/etc/turnserver.conf"]
