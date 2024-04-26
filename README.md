1. Build project:

   ```bash
   mvn clean package
   ```

2. Build Docker image:

   ```bash
   docker build -t load-testing .
   ```

3. Run the load test service using the Docker Compose:

   ```bash
   docker compose up
   ```
