"componentA" {
    system = "TEST_SYSTEM"
    componentDisplayName = "Component A"
    componentOwner = "owner"
    releaseManager = "owner"
    groupId = "corp.domain"
    vcsUrl = "ssh://git@git.domain.corp/test/componentA.git"
    solution = false
    jira {
        projectKey = 'TEST'
        lineVersionFormat = '$major.$minor'
        majorVersionFormat = '$major.$minor'
        releaseVersionFormat = '$major.$minor.$service'
        displayName = 'Component A'
    }
    distribution {
        external = true
        explicit = true
    }
}

"componentB" {
    system = "TEST_SYSTEM"
    componentDisplayName = "Component B"
    componentOwner = "owner"
    releaseManager = "owner"
    groupId = "corp.domain"
    vcsUrl = "ssh://git@git.domain.corp/test/componentB.git"
    solution = false
    jira {
        projectKey = 'TEST'
        lineVersionFormat = '$major.$minor'
        majorVersionFormat = '$major.$minor'
        releaseVersionFormat = '$major.$minor.$service'
        displayName = 'Component B'
    }
    distribution {
        external = true
        explicit = true
    }
}

"componentC" {
    system = "TEST_SYSTEM"
    componentDisplayName = "Component C"
    componentOwner = "owner"
    releaseManager = "owner"
    groupId = "corp.domain"
    vcsUrl = "ssh://git@git.domain.corp/test/componentC.git"
    solution = false
    jira {
        projectKey = 'TEST'
        lineVersionFormat = '$major.$minor'
        majorVersionFormat = '$major.$minor'
        releaseVersionFormat = '$major.$minor.$service'
        displayName = 'Component C'
    }
    distribution {
        external = true
        explicit = true
    }
}

"componentD" {
    system = "TEST_SYSTEM"
    componentDisplayName = "Component D"
    componentOwner = "owner"
    releaseManager = "owner"
    groupId = "corp.domain"
    vcsUrl = "ssh://git@git.domain.corp/test/componentD.git"
    solution = false
    jira {
        projectKey = 'TEST'
        lineVersionFormat = '$major.$minor'
        majorVersionFormat = '$major.$minor'
        releaseVersionFormat = '$major.$minor.$service'
        displayName = 'Component D'
    }
    distribution {
        external = true
        explicit = true
    }
}
