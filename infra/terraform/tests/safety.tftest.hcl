mock_provider "aws" {}
variables {
  key_pair_name     = "test-key"
  ssh_cidr          = "203.0.113.1/32"
  db_password       = "test-password-only"
  valkey_auth_token = "test-token-not-a-real-secret"
}
run "minimal_private_data_plane" {
  command = plan
  assert {
    condition     = length(aws_subnet.public) == 2 && length(aws_subnet.private) == 2
    error_message = "Two public and two private subnets are required."
  }
  assert {
    condition     = aws_instance.app.instance_type == "t4g.micro" && aws_instance.app.credit_specification[0].cpu_credits == "standard"
    error_message = "Keep the 1 GiB ARM instance and avoid surplus CPU credit charges."
  }
  assert {
    condition     = !aws_db_instance.main.publicly_accessible && !aws_db_instance.main.multi_az && aws_db_instance.main.storage_encrypted && aws_db_instance.main.deletion_protection
    error_message = "RDS must be private, encrypted, single AZ and deletion protected."
  }
  assert {
    condition     = aws_elasticache_replication_group.main.engine == "valkey" && aws_elasticache_replication_group.main.num_cache_clusters == 1 && aws_elasticache_replication_group.main.transit_encryption_enabled
    error_message = "Valkey must use exactly one node with TLS."
  }
  assert {
    condition     = aws_elasticache_replication_group.main.auth_token_update_strategy == "SET"
    error_message = "Do not allow a passwordless authentication transition."
  }
}
run "allow_public_ssh" {
  command = plan
  variables { ssh_cidr = "0.0.0.0/0" }
  assert {
    condition     = aws_vpc_security_group_ingress_rule.ssh.cidr_ipv4 == "0.0.0.0/0" && aws_vpc_security_group_ingress_rule.ssh.from_port == 22 && aws_vpc_security_group_ingress_rule.ssh.to_port == 22
    error_message = "SSH must allow all IPv4 addresses on port 22."
  }
}
run "reject_short_cache_password" {
  command = plan
  variables { valkey_auth_token = "short" }
  expect_failures = [var.valkey_auth_token]
}

run "reject_invalid_ssh_cidr" {
  command = plan
  variables { ssh_cidr = "not-a-cidr" }
  expect_failures = [var.ssh_cidr]
}
