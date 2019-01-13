pipelineJob('promote-master') {
    definition {
        cps {
            script(readFileFromWorkspace('Jenkinsfile-junit-generator.groovy'))
            sandbox()
        }
    }
}

pipelineJob('promote-master-trigger') {
    definition {
        cps {
            script(readFileFromWorkspace('Jenkinsfile-call-junit-generator.groovy'))
            sandbox()
        }
    }
}


// Start the job
queue('promote-master-trigger')
