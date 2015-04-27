package com.thomsonreuters.lsps.transmart.fixtures

/**
 * Date: 27.04.2015
 * Time: 13:03
 */
class StudyInfo {
    String id
    String name

    StudyInfo(String id, String name) {
        this.id = id
        this.name = name
    }

    StudyInfo withSuffix(String suffix) {
        return new StudyInfo("${id}${suffix}", "${name} ${suffix}")
    }
}