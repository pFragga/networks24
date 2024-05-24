# Check if the 10th shared directory exists, if not, create directories 1 to 10
if (-Not (Test-Path -Path "shared_directory_10")) {
    For ($i = 1; $i -le 10; $i++) {
        New-Item -ItemType Directory -Name "shared_directory_$i" -Force
    }
}

# Check if the third argument is provided
if ($args.Length -lt 3) {
    # Select a random directory between shared_directory_1 and shared_directory_6
    $randomDirIndex = Get-Random -Minimum 1 -Maximum 7
    $dir = "shared_directory_$randomDirIndex"

    # Run the Java program with default values for first and second arguments if they are not provided
    $hostname = if ($args.Length -ge 1) { $args[0] } else { "localhost" }
    $port = if ($args.Length -ge 2) { $args[1] } else { "9090" }
    java -cp bin PeerMain $hostname $port $dir
} else {
    # Run the Java program with all provided arguments
    java -cp bin PeerMain $args[0] $args[1] $args[2]
}

# Exit with the status code of the last executed command
exit $LASTEXITCODE
