#!/bin/bash
set -e

# Start GitLab
docker compose up -d

echo "GitLab is starting. This may take a few minutes..."
echo ""
echo "Once ready, access GitLab at: http://localhost"
echo ""
echo "To get the initial root password, run:"
echo "  docker exec -it gitlab grep 'Password:' /etc/gitlab/initial_root_password"
