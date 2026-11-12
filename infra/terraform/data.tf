resource "aws_db_subnet_group" "main" {
  name       = var.name
  subnet_ids = [for subnet in aws_subnet.private : subnet.id]
}
resource "aws_db_instance" "main" {
  identifier               = var.name
  engine                   = "mysql"
  engine_version           = var.mysql_engine_version
  engine_lifecycle_support = "open-source-rds-extended-support-disabled"
  instance_class           = "db.t4g.micro"
  allocated_storage        = 20
  storage_type             = "gp3"
  storage_encrypted        = true
  username                 = "root"
  password_wo              = var.db_password
  password_wo_version      = var.db_password_version
  # RDS initial DBName disallows hyphens. Create `moment-link` with SQL later.
  db_subnet_group_name       = aws_db_subnet_group.main.name
  vpc_security_group_ids     = [aws_security_group.db.id]
  availability_zone          = var.availability_zones[0]
  multi_az                   = false
  publicly_accessible        = false
  backup_retention_period    = 7
  backup_window              = "18:00-19:00"
  maintenance_window         = "sun:19:00-sun:20:00"
  auto_minor_version_upgrade = true
  deletion_protection        = true
  skip_final_snapshot        = false
  final_snapshot_identifier  = "${var.name}-final"
  copy_tags_to_snapshot      = true
}
