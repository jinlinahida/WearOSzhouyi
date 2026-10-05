pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.library") {
                useModule("com.android.tools.build:gradle:8.7.3")
            }
        }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Boompala"
include(":app")
include(":engine")

// ShirokoWear UI 源码轨：改库立刻在本应用生效，不需要先发版再验证。
// 坐标被替换为 included build 的工程，因此不经任何仓库解析。
includeBuild("../ShirokoWearUI") {
    dependencySubstitution {
        substitute(module("io.github.jinlinahida:shirokowear-ui"))
            .using(project(":shirokowear"))
        substitute(module("io.github.jinlinahida:shirokowear-navigation"))
            .using(project(":shirokowear-navigation"))
    }
}
