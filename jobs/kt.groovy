job('Giphy_Ingest_dev') {
pipeline {
    agent any
    stages {
        stage('Build') { 
            steps {
                     deleteDir()
    checkout([$class: 'GitSCM', branches: [[name: '*/dev' ]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '8ace4b6c-6b22-4341-8375-83dce7787b52', url: 'http://git.samsungmtv.com/anuj.prashar/giphy-trending-ingest.git']]]) 
            }
        }
                stage('Initialize version number') {
            steps {
                script {
                    sh "cd ${env.WORKSPACE}"
               sh '''
              export  M2_HOME=/usr/share/maven/
              export PATH=$M2_HOME/bin:$PATH
              mvn clean install -f pom.xml
              '''
                    def pom = readMavenPom file: 'pom.xml'
                    env.BUILD_PACKAGE_VERSION = pom.version
                }
                echo "Build version: ${env.BUILD_PACKAGE_VERSION}"
            }
        }
          stage('SonarAnalysis') {
            steps {
    withSonarQubeEnv('SonarQube'){
               
       sh "cd ${env.WORKSPACE}"
        sh '''
        export SONAR_HOME=/opt/sonar-scanner/sonar-runner-2.4/ 
        export PATH=$SONAR_HOME/bin:$PATH 
     sonar-runner -Dsonar.sourceEncoding=UTF-8 -Dsonar.projectKey=Giphy-Trending-Ingest -Dsonar.sources=src -Dsonar.language=java -Dsonar.java.binaries=target/classes

      '''
              }
            }
        }
   
               stage('Build service docker image') {
            steps {
                
                    script {
                        def pom = readMavenPom file: 'pom.xml'
                        env.DOCKER_PACKAGE_NAME = pom.artifactId
                        env.PACKAGE_FILE_NAME = "${env.DOCKER_PACKAGE_NAME}-${env.BUILD_PACKAGE_VERSION}.jar"
                        env.DOCKER_IMAGE_TAG = "${env.DOCKER_REGISTRY_HOST}/${env.DOCKER_PACKAGE_NAME}:${env.BUILD_PACKAGE_VERSION}"
                        env.DOCKER_LAST_IMAGE_TAG = "${env.DOCKER_REGISTRY_HOST}/${env.DOCKER_PACKAGE_NAME}:latest"
                    }
                    sh "docker image build --build-arg JAR_FILE=target/${env.PACKAGE_FILE_NAME} -t 251610726343.dkr.ecr.us-east-2.amazonaws.com/giphy-ingest:${env.BUILD_PACKAGE_VERSION} . "
                    
            }
        }
     
 stage('DockerLogin') {
            steps {
                sh '''
                 export AWS_PROFILE=seabheast2
                 export AWS_REGION=us-east-2
                echo $(aws ecr get-authorization-token --region us-east-2 --output text --query 'authorizationData[].authorizationToken' | base64 -d | cut -d: -f2) | docker login -u AWS 251610726343.dkr.ecr.us-east-2.amazonaws.com --password-stdin 
         '''
            }
        }
 stage('DockerPush') {
            steps {
                script {
                 def pom = readMavenPom file: 'pom.xml'
                        env.DOCKER_PACKAGE_NAME = pom.artifactId
                        env.PACKAGE_FILE_NAME = "${env.DOCKER_PACKAGE_NAME}-${env.BUILD_PACKAGE_VERSION}.jar"
                        env.DOCKER_IMAGE_TAG = "${env.DOCKER_REGISTRY_HOST}/${env.DOCKER_PACKAGE_NAME}:${env.BUILD_PACKAGE_VERSION}"
                        env.DOCKER_LAST_IMAGE_TAG = "${env.DOCKER_REGISTRY_HOST}/${env.DOCKER_PACKAGE_NAME}:latest"
                }
sh "docker push 251610726343.dkr.ecr.us-east-2.amazonaws.com/giphy-ingest:${env.BUILD_PACKAGE_VERSION}"            }
        }
        
        
        stage('removeImages') {
            steps {
                   script {
                 def pom = readMavenPom file: 'pom.xml'
                        env.DOCKER_PACKAGE_NAME = pom.artifactId
                        env.PACKAGE_FILE_NAME = "${env.DOCKER_PACKAGE_NAME}-${env.BUILD_PACKAGE_VERSION}.jar"
                        env.DOCKER_IMAGE_TAG = "${env.DOCKER_REGISTRY_HOST}/${env.DOCKER_PACKAGE_NAME}:${env.BUILD_PACKAGE_VERSION}"
                        env.DOCKER_LAST_IMAGE_TAG = "${env.DOCKER_REGISTRY_HOST}/${env.DOCKER_PACKAGE_NAME}:latest"
                }
                sh ''' 
                docker images -q| xargs docker rmi --force
         '''
            }
        }
 stage ('Download_ECS_DeployMentScript') {
     steps {
  sh '''
  
    wget https://s3.us-east-2.amazonaws.com/ecs-dev-deployment-playbooks/giphy-ingest-dev.yaml
        
  '''
 }
 }
    stage ('Create_and_DEPLOy_ECS'){
        steps {
    sh '''
    export AWS_PROFILE=seabheast2
    export AWS_REGION=us-east-2
    ansible-playbook giphy-ingest-dev.yaml -vvv
    '''
    }
    }
  stage ('Removeworkspace')
  { 
      steps {
      deleteDir()
      }
  }


        
          
        
    }
}
}
