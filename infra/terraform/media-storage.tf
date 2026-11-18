data "aws_caller_identity" "current" {}

# 버킷 이름은 전역 유일해야 하므로 계정 ID를 붙인다.
resource "aws_s3_bucket" "media" {
  bucket        = "${var.name}-media-${data.aws_caller_identity.current.account_id}"
  force_destroy = false
  tags          = { Name = "${var.name}-media" }
}

resource "aws_s3_bucket_public_access_block" "media" {
  bucket                  = aws_s3_bucket.media.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_ownership_controls" "media" {
  bucket = aws_s3_bucket.media.id
  rule { object_ownership = "BucketOwnerEnforced" }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "media" {
  bucket = aws_s3_bucket.media.id
  rule {
    apply_server_side_encryption_by_default { sse_algorithm = "AES256" }
  }
}

# 브라우저가 서명 URL로 직접 PUT(서명 헤더 포함)하고 XHR로 조회할 수 있게 한다.
resource "aws_s3_bucket_cors_configuration" "media" {
  bucket = aws_s3_bucket.media.id
  cors_rule {
    allowed_methods = ["PUT", "GET"]
    allowed_origins = var.media_cors_origins
    allowed_headers = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3600
  }
}

resource "aws_iam_role_policy" "media" {
  name = "${var.name}-media"
  role = aws_iam_role.app.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"]
        Resource = "${aws_s3_bucket.media.arn}/events/*"
      },
      {
        # HEAD에서 없는 객체를 403이 아닌 404로 구분하려면 필요하다.
        Effect   = "Allow"
        Action   = ["s3:ListBucket"]
        Resource = aws_s3_bucket.media.arn
      },
    ]
  })
}
