export interface Resource {
    name: string;
    url: string;
    description: string;
}

export interface TechResources {
    youtube: Resource[];
    courses: Resource[];
    podcasts: Resource[];
    websites: Resource[];
    projects: Resource[];
    people: Resource[];
}

export interface CommunityResources {
    meetups: Resource[];
    events: Resource[];
    localProjects: Resource[];
    celebrities: Resource[];
}

export const LEARN_RESOURCES: Record<string, TechResources> = {
    'android': {
        youtube: [
            { name: 'Android Developers', url: 'https://www.youtube.com/@AndroidDevelopers', description: 'Official Android developer channel' },
            { name: 'Philipp Lackner', url: 'https://www.youtube.com/@PhilippLackner', description: 'Deep dives into Kotlin and Compose' },
            { name: 'Stevdza-San', url: 'https://www.youtube.com/@StevdzaSan', description: 'Practical Android UI and library tutorials' },
        ],
        courses: [
            { name: 'Android Basics with Compose', url: 'https://developer.android.com/courses/android-basics-compose/course', description: 'Official Google course for beginners' },
            { name: 'Kotlin Bootcamp for Programmers', url: 'https://www.udacity.com/course/kotlin-bootcamp-for-programmers--ud9011', description: 'Essential Kotlin for Android' },
        ],
        podcasts: [
            { name: 'Fragmented Podcast', url: 'https://fragmentedpodcast.com/', description: 'Android developer podcast' },
            { name: 'Android Developers Backstage', url: 'https://adbackstage.libsyn.com/', description: 'Behind the scenes by Google engineers' },
        ],
        websites: [
            { name: 'Android Developers', url: 'https://developer.android.com/', description: 'The source of truth for Android API' },
            { name: 'Kotlin Documentation', url: 'https://kotlinlang.org/docs/home.html', description: 'Official Kotlin language guide' },
            { name: 'Android Weekly', url: 'https://androidweekly.net/', description: 'Newsletter for Android developers' },
        ],
        projects: [
            { name: 'Now in Android', url: 'https://github.com/android/nowinandroid', description: 'A fully functional Android app built entirely with Kotlin and Jetpack Compose' },
            { name: 'Architecture Blueprints', url: 'https://github.com/android/architecture-samples', description: 'Samples to demonstrate different architectural approaches' },
        ],
        people: [
            { name: 'Hadi Hariri', url: 'https://twitter.com/hhariri', description: 'VP of Developer Advocacy at JetBrains' },
            { name: 'Jake Wharton', url: 'https://twitter.com/jakewharton', description: 'Legendary Android developer and contributor' },
        ]
    },
    'ios': {
        youtube: [
            { name: 'Apple Developer', url: 'https://www.youtube.com/@AppleDeveloper', description: 'Official Apple developer channel' },
            { name: 'Sean Allen', url: 'https://www.youtube.com/@seanallen', description: 'iOS development tutorials and career advice' },
            { name: 'Kavsoft', url: 'https://www.youtube.com/@Kavsoft', description: 'Advanced SwiftUI layouts and animations' },
        ],
        courses: [
            { name: '100 Days of SwiftUI', url: 'https://www.hackingwithswift.com/100/swiftui', description: 'Free comprehensive SwiftUI course by Paul Hudson' },
            { name: 'Developing Apps for iOS', url: 'https://cs193p.sites.stanford.edu/', description: 'Stanford University\'s famous iOS course' },
        ],
        podcasts: [
            { name: 'Swift over Coffee', url: 'https://www.hackingwithswift.com/podcast', description: 'Conversations about Swift and iOS development' },
            { name: 'Under the Radar', url: 'https://www.relay.fm/undertheradar', description: 'Independent app development insights' },
        ],
        websites: [
            { name: 'Apple Developer Documentation', url: 'https://developer.apple.com/documentation/', description: 'Official reference for Apple frameworks' },
            { name: 'Hacking with Swift', url: 'https://www.hackingwithswift.com/', description: 'Hands-on Swift tutorials' },
            { name: 'Swift.org', url: 'https://www.swift.org/', description: 'Official home of the Swift language' },
        ],
        projects: [
            { name: 'Awesome iOS', url: 'https://github.com/vsouza/awesome-ios', description: 'A curated list of awesome iOS ecosystems' },
            { name: 'Swift Algorithms', url: 'https://github.com/apple/swift-algorithms', description: 'Standard library algorithms by Apple' },
        ],
        people: [
            { name: 'Paul Hudson', url: 'https://twitter.com/twostraws', description: 'Creator of Hacking with Swift' },
            { name: 'Chris Lattner', url: 'https://twitter.com/clattner_llvm', description: 'The creator of the Swift programming language' },
        ]
    }
};

export const COMMUNITY_RESOURCES: Record<string, CommunityResources> = {
    'android': {
        meetups: [
            { name: 'Auckland Android User Group', url: 'https://www.meetup.com/auckland-android/', description: 'Local meetups for Android enthusiasts in Auckland' },
            { name: 'GDG Auckland', url: 'https://gdg.community.dev/gdg-auckland/', description: 'Google Developer Group Auckland' },
        ],
        events: [
            { name: 'DevFest Auckland', url: 'https://devfest.withgoogle.com/', description: 'Annual Google developer conference in Auckland' },
        ],
        localProjects: [
            { name: 'Kiwi Android GitHub Contributors', url: 'https://github.com/search?q=location%3AAuckland+language%3AKotlin', description: 'Explore Kotlin/Android projects by Auckland developers' },
        ],
        celebrities: [
            { name: 'Local Tech Leaders', url: '#', description: 'Prominent Android architects in the NZ tech scene' }
        ]
    },
    'ios': {
        meetups: [
            { name: 'Auckland iOS Developers', url: 'https://www.meetup.com/Auckland-iOS-Developers/', description: 'Auckland-based iOS developer community' },
            { name: 'CocoaHeads Auckland', url: 'https://www.meetup.com/CocoaHeads-Auckland/', description: 'Local chapter of the global CocoaHeads community' },
        ],
        events: [
            { name: '/dev/world', url: 'https://www.auc.edu.au/devworld/', description: 'The premier conference for Apple developers in Australasia' },
        ],
        localProjects: [
            { name: 'Kiwi iOS GitHub Contributors', url: 'https://github.com/search?q=location%3AAuckland+language%3ASwift', description: 'Explore Swift projects by Auckland-based developers' },
        ],
        celebrities: [
            { name: 'NZ iOS Experts', url: '#', description: 'Leading Swift developers in the New Zealand ecosystem' }
        ]
    }
};
