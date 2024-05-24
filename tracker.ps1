# Default port to 9090 if not provided
$port = if ($args.Length -ge 1) { $args[0] } else { "9090" }

# Run the Java program with the specified or default port
java -cp bin TrackerMain $port

# Exit with the status code of the last executed command
exit $LASTEXITCODE
