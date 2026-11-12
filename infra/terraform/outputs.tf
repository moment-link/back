output "public_ip" { value = aws_eip.app.public_ip }
output "instance_id" { value = aws_instance.app.id }
output "db_host" { value = aws_db_instance.main.address }
output "db_name" { value = "moment-link" }
output "ssh_command" { value = "ssh -i /path/to/your.pem ec2-user@${aws_eip.app.public_ip}" }
