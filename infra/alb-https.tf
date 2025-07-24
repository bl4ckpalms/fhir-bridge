# HTTPS Listener for ALB
resource "aws_lb_listener" "fhir_bridge_https" {
  load_balancer_arn = aws_lb.main.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = aws_acm_certificate.main.arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.fhir_bridge.arn
  }
}

# HTTP to HTTPS redirect
resource "aws_lb_listener_rule" "redirect_http_to_https" {
  listener_arn = aws_lb_listener.fhir_bridge.arn
  priority     = 100

  action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }

  condition {
    path_pattern {
      values = ["/*"]
    }
  }
}

# Security group rule for HTTPS
resource "aws_security_group_rule" "alb_https" {
  type              = "ingress"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
  security_group_id = aws_security_group.alb.id
}