mock_provider "aws" {}
variables {
  key_pair_name = "test-key"
  ssh_cidr      = "203.0.113.1/32"
  db_password   = "test-password-only"
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
}
run "allow_public_ssh" {
  command = plan
  variables { ssh_cidr = "0.0.0.0/0" }
  assert {
    condition     = aws_vpc_security_group_ingress_rule.ssh.cidr_ipv4 == "0.0.0.0/0" && aws_vpc_security_group_ingress_rule.ssh.from_port == 22 && aws_vpc_security_group_ingress_rule.ssh.to_port == 22
    error_message = "SSH must allow all IPv4 addresses on port 22."
  }
}
run "reject_invalid_ssh_cidr" {
  command = plan
  variables { ssh_cidr = "not-a-cidr" }
  expect_failures = [var.ssh_cidr]
}
