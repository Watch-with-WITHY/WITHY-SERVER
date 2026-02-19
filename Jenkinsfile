pipeline {
    agent any

    parameters {
        choice(
            name: 'DDL_AUTO',
            choices: ['update', 'create', 'validate', 'none'],
            description: '⚠️ create 선택 시 DB 데이터 초기화됨! (평소엔 update/validate)'
        )
    }

    tools {
        dockerTool 'docker-tool'
    }

    environment {
        DOCKER_IMAGE_NAME = 'eungho/withy'

        // Credentials
        DOCKER_CRED = credentials('docker-hub-cred')
        DB_CRED = credentials('db-cred')

        // AWS & Google Key
        S3_ACCESS_KEY = credentials('s3-access-key')
        S3_SECRET_KEY = credentials('s3-secret-key')
        S3_URL = credentials('s3-url')

        // Google Keys
        GOOGLE_CLIENT_ID = credentials('GOOGLE_CLIENT_ID')
        GOOGLE_CLIENT_SECRET = credentials('GOOGLE_CLIENT_SECRET')
        GOOGLE_REDIRECT_URI = credentials('GOOGLE_REDIRECT_URI')
        GOOGLE_EMAIL_USERNAME = credentials('google-email-username')
        GOOGLE_EMAIL_PASSWORD = credentials('google-email-password')

        // Other Secrets
        JWT_SECRET_KEY = credentials('JWT_SECRET_KEY')
        JENKINS_TMDB_KEY = credentials('tmdb-api-key')
        JENKINS_YOUTUBE_KEY = credentials('youtube-api-key')

        // Infra Credentials
        SERVER_IP = credentials('prod-server-ip')
        PROD_REDIS_PASSWORD = credentials('prod-redis-password')
        
        // AI Server Info (gRPC & Spoiler)
        AI_GRPC_ADDRESS = credentials('ai-grpc-address')
        AI_SPOILER_URL = credentials('ai-spoiler-url')
        AI_REFINEMENT_URL = credentials('ai-refinement-url')
        AI_RECOMMENDATION_URL = credentials('ai-recommendation-url')
        AI_API_KEY = credentials('AI_API_KEY')

        // DB Info (String)
        DB_URL = "jdbc:mysql://mysql:3306/withy?serverTimezone=Asia/Seoul\\&characterEncoding=UTF-8"
        DB_USERNAME = "root"
        S3_BUCKET_NAME = "withy-images-2026"
    }

    stages {
        // [1] 빌드 & 테스트
        stage('Build Project') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew build'
            }
        }

        // [2] 도커 빌드 & 푸시 (develop-be 브랜치만)
        stage('Docker Build & Push') {
            when { expression { return env.GIT_BRANCH.endsWith('develop-be') } }
            steps {
                script {
                    sh "echo $DOCKER_CRED_PSW | docker login -u $DOCKER_CRED_USR --password-stdin"
                    sh "docker build -t $DOCKER_IMAGE_NAME:latest ."
                    sh "docker push $DOCKER_IMAGE_NAME:latest"
                    sh "docker rmi $DOCKER_IMAGE_NAME:latest"
                }
            }
        }

        // [3] 통합 배포 (인프라 + 앱 동시 배포)
        stage('Deploy to Prod') {
            when { expression { return env.GIT_BRANCH.endsWith('develop-be') } }
            steps {
                sshagent(credentials: ['ec2-ssh-key']) {
                    sh '''
                        echo "🚀 [배포] 운영 서버 통합 배포 시작..."

                        # 1. 파일 전송 (docker-compose.yml)
                        ssh -o StrictHostKeyChecking=no ubuntu@$SERVER_IP "mkdir -p /home/ubuntu/withy-infra"
                        scp -o StrictHostKeyChecking=no docker-compose-prod.yml ubuntu@$SERVER_IP:/home/ubuntu/withy-infra/
                        scp -r -o StrictHostKeyChecking=no nginx ubuntu@$SERVER_IP:/home/ubuntu/withy-infra/

                        # 2. 원격 서버 접속
                        ssh -o StrictHostKeyChecking=no ubuntu@$SERVER_IP "
                            cd /home/ubuntu/withy-infra

                            # 2-1. 도커 허브 로그인 (Private 이미지 접근용)
                            echo $DOCKER_CRED_PSW | docker login -u $DOCKER_CRED_USR --password-stdin

                            # 2-2. .env 파일 생성 (환경변수 주입 - 이게 핵심!)
                            # docker-compose가 이 파일을 읽어서 ${VAR} 부분을 채워줌
                            echo \"DDL_AUTO=$DDL_AUTO\" > .env
                            echo \"DB_URL=$DB_URL\" >> .env
                            echo \"DB_USERNAME=$DB_USERNAME\" >> .env
                            echo \"DB_PASSWORD=$DB_CRED_PSW\" >> .env

                            echo \"S3_ACCESS_KEY=$S3_ACCESS_KEY\" >> .env
                            echo \"S3_SECRET_KEY=$S3_SECRET_KEY\" >> .env
                            echo \"S3_BUCKET_NAME=$S3_BUCKET_NAME\" >> .env
                            echo \"S3_URL=$S3_URL\" >> .env

                            echo \"GOOGLE_CLIENT_ID=$GOOGLE_CLIENT_ID\" >> .env
                            echo \"GOOGLE_CLIENT_SECRET=$GOOGLE_CLIENT_SECRET\" >> .env
                            echo \"GOOGLE_REDIRECT_URI=$GOOGLE_REDIRECT_URI\" >> .env
                            echo \"GOOGLE_EMAIL_USERNAME=$GOOGLE_EMAIL_USERNAME\" >> .env
                            echo \"GOOGLE_EMAIL_PASSWORD=$GOOGLE_EMAIL_PASSWORD\" >> .env

                            echo \"TMDB_API_KEY=$JENKINS_TMDB_KEY\" >> .env
                            echo \"YOUTUBE_API_KEY=$JENKINS_YOUTUBE_KEY\" >> .env
                            echo \"JWT_SECRET_KEY=$JWT_SECRET_KEY\" >> .env
                            echo \"AI_API_KEY=$AI_API_KEY\" >> .env

                            echo \"REDIS_PASSWORD=$PROD_REDIS_PASSWORD\" >> .env
                            echo \"SERVER_IP=$SERVER_IP\" >> .env

                            echo \"AI_GRPC_ADDRESS=$AI_GRPC_ADDRESS\" >> .env
                            echo \"AI_SPOILER_URL=$AI_SPOILER_URL\" >> .env
                            echo \"AI_REFINEMENT_URL=$AI_REFINEMENT_URL\" >> .env
                            echo \"AI_RECOMMENDATION_URL=$AI_RECOMMENDATION_URL\" >> .env

                            # 2-3. 최신 이미지 Pull (Backend)
                            docker-compose -f docker-compose-prod.yml pull backend

                            # 2-4. 컨테이너 실행 (변경된 것만 재시작됨)
                            docker-compose -f docker-compose-prod.yml up -d

                            # 2-5. 미사용 이미지 정리
                            docker image prune -f
                        "
                    '''
                }
            }
        }
    }
}