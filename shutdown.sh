SPRING_PID=$(ps aux | grep "spring-boot-web-app.jar" | grep -v grep | awk '{print $2}' | head -n 1)
if [ -n "$SPRING_PID" ]; then
  echo "Stopping Spring Boot app (PID: $SPRING_PID)..."
  kill -TERM $SPRING_PID
  sleep 3
  # Force kill only if still running
  if kill -0 $SPRING_PID 2>/dev/null; then
    echo "Force stopping Spring Boot app..."
    kill -9 $SPRING_PID
  fi
  sleep 2
fi