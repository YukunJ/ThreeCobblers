provider "aws" {
  region = "us-east-1"
}

##############################################################
# Data sources to get VPC, subnets and security group details
##############################################################
data "aws_vpc" "eks" {
  id = "???"
}

data "aws_subnet_ids" "eks" {
  vpc_id = "${data.aws_vpc.eks.id}"
}

resource "aws_db_subnet_group" "eks" {
  name       = "db_subnet_grop_name"
  subnet_ids = data.aws_subnet_ids.eks.ids
  tags = {
    Project = "twitter-phase-3"
  }
}

#####
# DB
#####
resource "aws_db_instance" "default" {

  engine            = "mysql"
  engine_version    = "8.0.23"
  instance_class    = "db.r6g.xlarge"
  storage_type      = "gp2"
  allocated_storage = 150

  db_name     = "???"
  username = "???"
  password = "???"
  port     = "3306"

  // the security group associated with EKS cluster
  vpc_security_group_ids = ["???", "???", "???", "???"]

  tags = {
    Project = "twitter-phase-3"
  }
  parameter_group_name            = "phase3"
  availability_zone = "us-east-1a"
  skip_final_snapshot  = true
  db_subnet_group_name = "${aws_db_subnet_group.eks.id}"

}
