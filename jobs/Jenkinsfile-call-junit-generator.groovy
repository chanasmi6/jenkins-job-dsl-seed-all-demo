
#!/usr/bin/env groovy

node("master") {
    def builds = [:]
    for (int i = 0; i < 10; i++) {

        int index = i

        builds[index] = {
            build job: 'promote-master', wait: true, propagate: false, quietPeriod: 0
        }
    }

    stage("Run promote builds") {
        parallel builds
    }
}
