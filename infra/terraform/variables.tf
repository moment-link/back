variable "region" {
  type    = string
  default = "ap-northeast-2"
}
variable "name" {
  type    = string
  default = "moment-link"
  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{1,29}[a-z0-9]$", var.name))
    error_message = "Use 3-31 lowercase letters, digits and internal hyphens."
  }
}
variable "availability_zones" {
  type    = list(string)
  default = ["ap-northeast-2a", "ap-northeast-2c"]
  validation {
    condition     = length(var.availability_zones) == 2 && length(distinct(var.availability_zones)) == 2
    error_message = "Exactly two distinct availability zones are required."
  }
}
variable "key_pair_name" {
  type        = string
  description = "Existing EC2 key pair in this region; download its PEM yourself at creation."
}
variable "ssh_cidr" {
  type        = string
  default     = "0.0.0.0/0"
  description = "IPv4 CIDR allowed to access SSH; all IPv4 addresses by user request."
  validation {
    condition     = can(cidrnetmask(var.ssh_cidr))
    error_message = "SSH CIDR must be a valid IPv4 CIDR."
  }
}
variable "db_password" {
  type        = string
  sensitive   = true
  ephemeral   = true
  description = "Input locally. Passed to RDS using a write-only argument."
  validation {
    condition     = length(var.db_password) >= 8 && length(var.db_password) <= 41 && can(regex("^[!-~]+$", var.db_password)) && length(regexall("[/@\"]", var.db_password)) == 0
    error_message = "RDS password must be 8-41 printable ASCII characters excluding slash, at sign and double quote."
  }
}
variable "db_password_version" {
  type        = number
  default     = 1
  description = "Increment when changing the write-only DB password."
}
variable "valkey_auth_token" {
  type        = string
  sensitive   = true
  description = "16-128 characters; stored in protected Terraform state."
  validation {
    condition     = can(regex("^[A-Za-z0-9!&#$^<>-]{16,128}$", var.valkey_auth_token))
    error_message = "Use 16-128 letters, digits or ! & # $ ^ < > - characters."
  }
}
variable "mysql_engine_version" {
  type    = string
  default = "8.4"
}
variable "valkey_engine_version" {
  type    = string
  default = "7.2"
}
