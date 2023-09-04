multibranchPipelineJob('Nanoware/TeraNUI') {
    description("Builds our UI framework")
    branchSources {
        github {
            id('teranuimultipipenanoware') // IMPORTANT: use a constant and unique identifier
            repoOwner('Nanoware')
            repository('TeraNUI')
            checkoutCredentialsId('gh-app-nanoware')
            scanCredentialsId('gh-app-nanoware')
            buildOriginPRMerge(true) // We want to build PRs in the origin, merge with base branch
            buildOriginBranchWithPR(false) // We don't keep both an origin branch and a PR from it
            buildForkPRHead(false) // We only build forks merged into the base branch
        }
    }
    orphanedItemStrategy {
        defaultOrphanedItemStrategy {
            pruneDeadBranches(true)
            daysToKeepStr("1") // Only keep orphaned PRs and branches for a day (yes, requires a String)
            numToKeepStr("1") // Only keep 1 of the orphans? Unsure what this means exactly
        }
    }
    triggers {
        periodic(24 * 60)  // Scan for folder updates once a day, otherwise may only trigger when jobs are re-saved
    }
}
