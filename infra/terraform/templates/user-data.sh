#!/bin/bash
set -euo pipefail
# EIP reassociation can briefly interrupt boot-time Internet connectivity.
for attempt in $(seq 1 12); do
  if dnf install -y docker; then break; fi
  sleep 10
done
rpm -q docker
systemctl enable --now docker
usermod -aG docker ec2-user
install -d -m 0750 -o ec2-user -g ec2-user /opt/moment-link
# Require public-key authentication even though SSH ingress is public.
install -d -m 0755 /etc/ssh/sshd_config.d
cat > /etc/ssh/sshd_config.d/00-moment-link.conf <<'SSH'
PubkeyAuthentication yes
PasswordAuthentication no
KbdInteractiveAuthentication no
AuthenticationMethods publickey
PermitRootLogin no
SSH
sshd -t
systemctl reload sshd
# No application secrets or SSH private keys are embedded in user data.
cat > /etc/docker/daemon.json <<'JSON'
{"log-driver":"local","log-opts":{"max-size":"10m","max-file":"3"}}
JSON
systemctl restart docker
if [ ! -f /swapfile ]; then
  fallocate -l 1G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
  echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi
