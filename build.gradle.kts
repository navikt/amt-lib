// fjernes ved neste release av org.apache.kafka:kafka-clients
subprojects {
    configurations.configureEach {
        resolutionStrategy {
            capabilitiesResolution {
                withCapability("org.lz4:lz4-java") {
                    select(candidates.first { (it.id as ModuleComponentIdentifier).group == "at.yawk.lz4" })
                }
            }
        }
    }
}
