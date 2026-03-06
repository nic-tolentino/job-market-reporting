export interface Resource {
    title: string;
    url: string;
    description: string;
    previewImage?: string; // Optional thumbnail/preview image
    imageUrl?: string;     // Direct image link for the resource
    location?: string;     // e.g. "Global", "Auckland", "Wellington", "Alexandra"
    subscribers?: string;  // YouTube subscriber count
    stars?: string;        // GitHub stars count
    date?: string;         // Concrete date for events
    countries?: string[];  // e.g. ["NZ"], ["AU"], ["Global"]
}

export interface TechResources {
    youtube: Resource[];
    courses: Resource[];
    podcasts: Resource[];
    websites: Resource[];
    projects: Resource[];
    people: Resource[];
    communities: Resource[]; // Global level communities
}

export interface CommunityResources {
    communities: Resource[];      // Long-standing groups, meetups, orgs
    upcomingEvents: Resource[];   // Concrete events with dates
    localProjects: Resource[];
    localExperts: Resource[];     // Renamed from celebrities
}

export const LEARN_RESOURCES: Record<string, TechResources> = {
    android: {
        youtube: [
            { title: "Fireship", url: "https://www.youtube.com/@Fireship", description: "Quick 'Code in 100 Seconds' for Android tools.", location: "Global", subscribers: "4.1M" },
            { title: "Android Developers", url: "https://www.youtube.com/@AndroidDevelopers", description: "Official Android developer channel (Global Insights)", previewImage: "https://www.gstatic.com/android/devsite/56d0d5d5a9d4e5d5/android/images/lockup_android_developers.png", location: "Global", subscribers: "1.1M" },
            { title: "Philipp Lackner", url: "https://www.youtube.com/@PhilippLackner", description: "Jetpack Compose & Architecture expert.", location: "Global", subscribers: "244k" },
            { title: "Coding with Mitch", url: "https://www.youtube.com/@mitchtabian", description: "Deep dives into clean architecture and testing.", location: "Global", subscribers: "145k" },
            { title: "Rahul Pandey", url: "https://www.youtube.com/@rpandey1234", description: "Practical Android development and career advice.", location: "Global", subscribers: "125k" },
            { title: "Stevdza-San", url: "https://www.youtube.com/@StevdzaSan", description: "Fast-paced Android tutorials and tips.", location: "Global", subscribers: "95k" },
            { title: "Coding in Flow", url: "https://www.youtube.com/@CodinginFlow", description: "Straight-to-the-point Android guides.", location: "Global", subscribers: "120k" },
            { title: "The Android Factory", url: "https://www.youtube.com/@TheAndroidFactory", description: "Architecture and design patterns in Kotlin.", location: "Global", subscribers: "15k" },
            { title: "Practical Coding", url: "https://www.youtube.com/@PracticalCoding", description: "Functional programming in Android.", location: "Global", subscribers: "12k" },
            { title: "Chet Haase", url: "https://www.youtube.com/@chethaase", description: "Android team veteran on UI and Graphics.", location: "Global", subscribers: "10k" }
        ].sort((a, b) => {
            const getVal = (s?: string) => s ? (s.includes('M') ? parseFloat(s) * 1000 : parseFloat(s)) : 0;
            return getVal(b.subscribers) - getVal(a.subscribers);
        }),
        courses: [
            { title: "Android Basics with Compose", url: "https://developer.android.com/courses/android-basics-compose/course", description: "The definitive guide for modern Android", previewImage: "https://developer.android.com/static/images/courses/android-basics-compose/banner.png", location: "Global" },
            { title: "Developing Android Apps with Kotlin", url: "https://www.udacity.com/course/developing-android-apps-with-kotlin--ud9012", description: "Deep dive into production-ready Kotlin apps", location: "Global" },
            { title: "Jetpack Compose for Android", url: "https://www.kodeco.com/android/courses/jetpack-compose-for-android", description: "Comprehensive Compose course by Kodeco.", location: "Global" },
            { title: "Modern Android App Development", url: "https://www.coursera.org/specializations/android-app-development", description: "University-grade Android curriculum.", location: "Global" },
            { title: "Meta Android Developer", url: "https://www.coursera.org/professional-certificates/meta-android-developer", description: "Professional certification from Meta engineers.", location: "Global" },
            { title: "FreeCodeCamp Android Course", url: "https://www.youtube.com/watch?v=fis26HvvDII", description: "12-hour comprehensive Android crash course.", location: "Global" },
            { title: "Android Jetpack Masterclass", url: "https://www.udemy.com/course/android-jetpack-architecture-components-masterclass/", description: "Deep dive into Architecture Components.", location: "Global" },
            { title: "Android Application Development", url: "https://www.mygreatlearning.com/academy/learn-for-free/courses/android-application-development", description: "Foundational Android Studio concepts.", location: "Global" }
        ],
        podcasts: [
            { title: "Android Faithful", url: "https://androidfaithful.com/", description: "Cutting-edge Android news and banter", location: "Global" },
            { title: "Fragmented", url: "https://fragmentedpodcast.com/", description: "Architecture and software engineering for Android", location: "Global" },
            { title: "Android Backstage", url: "https://adbackstage.libsyn.com/", description: "Direct insights from the Android team at Google.", location: "Global" },
            { title: "The Android Show", url: "https://developer.android.com/android-show", description: "Official updates on Android development.", location: "Global" },
            { title: "AppForce1", url: "https://appforce1.net/", description: "Weekly mobile development news and interviews.", location: "Global" },
            { title: "Firebase Podcast", url: "https://firebase.google.com/podcast", description: "Backend for mobile news and features.", location: "Global" },
            { title: "Google Cloud Podcast", url: "https://www.gcppodcast.com/", description: "Cloud infrastructure for mobile apps.", location: "Global" },
            { title: "Mobile Dev Memo", url: "https://mobiledevmemo.com/", description: "Business and marketing side of mobile apps.", location: "Global" }
        ],
        websites: [
            { title: "Android Weekly", url: "https://androidweekly.net/", description: "Curated newsletter followed by millions", location: "Global" },
            { title: "Kotlin Documentation", url: "https://kotlinlang.org/docs/home.html", description: "The source of truth for the language", previewImage: "https://kotlinlang.org/assets/images/twitter-card.png", location: "Global" },
            { title: "Jetpack Compose Samples", url: "https://developer.android.com/jetpack/compose/samples", description: "Official Google sample apps and architectures.", location: "Global" },
            { title: "ProAndroidDev", url: "https://proandroiddev.com/", description: "Member-contributed Android technical articles.", location: "Global" },
            { title: "Styling Android", url: "https://blog.stylingandroid.com/", description: "Deep dives into Android UI and styling by Mark Allison.", location: "Global" },
            { title: "Kodeco (Ray Wenderlich)", url: "https://www.kodeco.com/android", description: "The highest quality tutorials for mobile devs.", location: "Global" },
            { title: "Android Developers Blog", url: "https://android-developers.googleblog.com/", description: "Official technical announcements and roadmap.", location: "Global" },
            { title: "Compose Academy", url: "https://compose.academy/", description: "Deep library of Jetpack Compose components and usage.", location: "Global" }
        ],
        projects: [
            { title: "OkHttp", url: "https://github.com/square/okhttp", description: "The underlying HTTP client for almost all apps.", location: "Global", stars: "47k" },
            { title: "Retrofit", url: "https://github.com/square/retrofit", description: "Industry standard type-safe HTTP client.", location: "Global", stars: "44k" },
            { title: "LeakCanary", url: "https://github.com/square/leakcanary", description: "The essential memory leak detection library.", location: "Global", stars: "30k" },
            { title: "ExoPlayer", url: "https://github.com/google/ExoPlayer", description: "Powerful media player for Android.", location: "Global", stars: "22k" },
            { title: "Now in Android", url: "https://github.com/android/nowinandroid", description: "Best-practice sample for modern architectures", previewImage: "https://github.com/android/nowinandroid/raw/main/tools/screenshots/main.png", location: "Global", stars: "21k" },
            { title: "Kivy", url: "https://github.com/kivy/kivy", description: "Open source Python framework for mobile apps.", location: "Global", stars: "19k" },
            { title: "Coil", url: "https://github.com/coil-kt/coil", description: "Kotlin-first image loading library.", location: "Global", stars: "12k" },
            { title: "Timber", url: "https://github.com/JakeWharton/timber", description: "Essential logging utility for Android.", location: "Global", stars: "11k" }
        ].sort((a, b) => {
            const getVal = (s?: string) => s ? parseFloat(s) : 0;
            return getVal(b.stars) - getVal(a.stars);
        }),
        people: [
            { title: "Jake Wharton", url: "https://jakewharton.com/", description: "Android legend, creator of Timber and OkHttp.", previewImage: "https://avatars.githubusercontent.com/u/66577?v=4", location: "Global" },
            { title: "Mark Murphy", url: "https://commonsware.com/", description: "Founder of CommonsWare and Android author.", previewImage: "https://commonsware.com/images/commonsware_logo.png", location: "Global" },
            { title: "Romain Guy", url: "https://twitter.com/romainguy", description: "Google Android veteran and graphics guru.", previewImage: "https://avatars.githubusercontent.com/u/849643?v=4", location: "Global" },
            { title: "Chet Haase", url: "https://twitter.com/chethaase", description: "Long-time Android team lead and humorist.", location: "Global" },
            { title: "Annyce Davis", url: "https://twitter.com/annyce", description: "Android GDE and Engineering Director at Meetup.", location: "Global" },
            { title: "Yigit Boyar", url: "https://twitter.com/yigitboyar", description: "Google Engineer, architect of Room and WorkManager.", location: "Global" },
            { title: "Huyen Tue Dao", url: "https://twitter.com/huyen_tue_dao", description: "Android GDE and frequent developer speaker.", location: "Global" },
            { title: "Donn Felker", url: "https://twitter.com/donnfelker", description: "Co-host of Fragmented and Android consultant.", location: "Global" },
            { title: "Philipp Lackner", url: "https://github.com/philipplackner", description: "Influential Jetpack Compose and architecture teacher.", location: "Global" }
        ],
        communities: [
            { title: "Kotlin Slack", url: "https://kotlinlang.slack.com/", description: "The definitive real-time community for Kotlin & Android.", location: "Global" },
            { title: "r/androiddev", url: "https://www.reddit.com/r/androiddev/", description: "Huge Reddit community for Android developers.", location: "Global" },
            { title: "Android United Slack", url: "https://androidunited.slack.com/", description: "Independent professional Android community.", location: "Global" },
            { title: "Google Developers Experts", url: "https://developers.google.com/community/experts", description: "Global network of highly experienced technology experts.", location: "Global" },
            { title: "Medium - Android Developers", url: "https://medium.com/androiddevelopers", description: "Technical stories from the people who build Android.", location: "Global" }
        ]
    },
    ios: {
        youtube: [
            { title: "Swiftful Thinking", url: "https://www.youtube.com/@SwiftfulThinking", description: "Advanced SwiftUI and architecture by Nick Sarno.", location: "Global", subscribers: "281k" },
            { title: "Apple Developer", url: "https://www.youtube.com/@AppleDeveloper", description: "Official Apple resources and insights.", location: "Global", subscribers: "259k" },
            { title: "LetsBuildThatApp", url: "https://www.youtube.com/@LetsBuildThatApp", description: "Deep dives into complex coding patterns.", location: "Global", subscribers: "200k" },
            { title: "Sean Allen", url: "https://www.youtube.com/@seanallen", description: "Career tips & iOS interview prep", location: "Global", subscribers: "172k" },
            { title: "CodeWithChris", url: "https://www.youtube.com/@CodeWithChris", description: "Beginner-friendly iOS path.", location: "Global", subscribers: "130k" },
            { title: "iOS Academy", url: "https://www.youtube.com/@iOSAcademy", description: "Step-by-step full app builds.", location: "Global", subscribers: "124k" },
            { title: "Kavsoft", url: "https://www.youtube.com/@Kavsoft", description: "Modern complex UI designs in SwiftUI.", location: "Global", subscribers: "55k" },
            { title: "Essential Developer", url: "https://www.youtube.com/@EssentialDeveloper", description: "Architecture and testing for pros.", location: "Global", subscribers: "42k" },
            { title: "Brian Advent", url: "https://www.youtube.com/@BrianAdvent", description: "In-depth Swift and Apple platform tutorials.", location: "Global", subscribers: "35k" }
        ].sort((a, b) => {
            const getVal = (s?: string) => s ? (s.includes('k') ? parseFloat(s) : parseFloat(s) * 1000) : 0;
            return getVal(b.subscribers) - getVal(a.subscribers);
        }),
        courses: [
            { title: "100 Days of SwiftUI", url: "https://www.hackingwithswift.com/100/swiftui", description: "Community-loved free course by Paul Hudson", previewImage: "https://www.hackingwithswift.com/img/100-days-of-swiftui.png", location: "Global" },
            { title: "Stanford CS193P", url: "https://cs193p.sites.stanford.edu/", description: "The prestigious iOS development course from Stanford.", location: "Global" },
            { title: "iOS & Swift - Bootcamp", url: "https://www.udemy.com/course/ios-13-app-development-bootcamp/", description: "The classic full-stack iOS curriculum", location: "Global" },
            { title: "SwiftUI Thinking Basics", url: "https://swiftfulthinking.com/", description: "Comprehensive foundation for modern iOS UI.", location: "Global" },
            { title: "iOS Developer Roadmap", url: "https://roadmap.sh/ios", description: "Visual guide to becoming an iOS expert.", location: "Global" },
            { title: "Develop in Swift", url: "https://developer.apple.com/learn/curriculum/", description: "Apple's official educational program.", location: "Global" },
            { title: "Advanced Swift Architecture", url: "https://www.pointfree.co/", description: "Functional programming and advanced Swift architecture.", location: "Global" },
            { title: "Lighthouse Labs iOS", url: "https://www.lighthouselabs.ca/en/ios-development-course", description: "Build real-world apps with Swift.", location: "Global" }
        ],
        podcasts: [
            { title: "Swift over Coffee", url: "https://www.hackingwithswift.com/podcast", description: "Relaxed discussion for the Apple ecosystem", location: "Global" },
            { title: "Under the Radar", url: "https://www.relay.fm/undertheradar", description: "Independent app development and business", location: "Global" },
            { title: "Accidental Tech Podcast", url: "https://atp.fm/", description: "In-depth technical discussions on Apple.", location: "Global" },
            { title: "Stacktrace", url: "https://9to5mac.com/shows/stacktrace/", description: "Weekly Apple news and dev talk.", location: "Global" },
            { title: "Swift by Sundell", url: "https://www.swiftbysundell.com/podcast", description: "Deep dives into Swift with experts.", location: "Global" },
            { title: "The Talk Show", url: "https://daringfireball.net/thetalkshow/", description: "John Gruber's definitive Apple commentary.", location: "Global" },
            { title: "MacBreak Weekly", url: "https://twit.tv/shows/macbreak-weekly", description: "Full Apple ecosystem news cycle.", location: "Global" },
            { title: "iOS Dev Break", url: "https://iosdevbreak.com/", description: "Quick insights for mobile developers.", location: "Global" }
        ],
        websites: [
            { title: "Swift.org", url: "https://www.swift.org/", description: "Official source of the Swift revolution", previewImage: "https://www.swift.org/favicon.ico", location: "Global" },
            { title: "Hacking with Swift", url: "https://www.hackingwithswift.com/", description: "Massive library of tutorials and guides", location: "Global" },
            { title: "Point-Free", url: "https://www.pointfree.co/", description: "Advanced functional programming in Swift.", location: "Global" },
            { title: "SwiftwithMajid", url: "https://swiftwithmajid.com/", description: "High-quality SwiftUI and architecture articles.", location: "Global" },
            { title: "SwiftSenpai", url: "https://swiftsenpai.com/", description: "Practical iOS tutorials and tips from Lee Kah Seng.", location: "Global" },
            { title: "objc.io", url: "https://www.objc.io/", description: "Advanced iOS and macOS development resources.", location: "Global" },
            { title: "AppCoda", url: "https://www.appcoda.com/", description: "Quality tutorials for iOS developers.", location: "Global" },
            { title: "NSHipster", url: "https://nshipster.com/", description: "Deep dives into the obscure parts of Cocoa/Swift.", location: "Global" }
        ],
        projects: [
            { title: "Alamofire", url: "https://github.com/Alamofire/Alamofire", description: "Elegant HTTP networking in Swift.", location: "Global", stars: "43k" },
            { title: "Charts", url: "https://github.com/danielgindi/Charts", description: "Beautiful charts for iOS apps.", location: "Global", stars: "28k" },
            { title: "Lottie iOS", url: "https://github.com/airbnb/lottie-ios", description: "Rendering native animations on iOS.", location: "Global", stars: "27k" },
            { title: "Kingfisher", url: "https://github.com/onevcat/Kingfisher", description: "Powerful image downloading and caching.", location: "Global", stars: "25k" },
            { title: "SnapKit", url: "https://github.com/SnapKit/SnapKit", description: "Popular Auto Layout DSL for Swift.", location: "Global", stars: "21k" },
            { title: "SkeletonView", url: "https://github.com/Juanpe/SkeletonView", description: "Elegant skeleton loading for iOS.", location: "Global", stars: "13k" },
            { title: "Wikipedia iOS", url: "https://github.com/wikimedia/wikipedia-ios", description: "High-quality reference for deep app features", previewImage: "https://github.com/wikimedia/wikipedia-ios/raw/main/Wikipedia/Images/AppIcon.png", location: "Global", stars: "4k" },
            { title: "IQKeyboardManager", url: "https://github.com/hackiftekhar/IQKeyboardManager", description: "Essential keyboard handling utility.", location: "Global", stars: "10k" }
        ].sort((a, b) => {
            const getVal = (s?: string) => s ? parseFloat(s) : 0;
            return getVal(b.stars) - getVal(a.stars);
        }),
        people: [
            { title: "Paul Hudson", url: "https://www.hackingwithswift.com/", description: "The creator of Hacking with Swift.", previewImage: "https://www.hackingwithswift.com/img/paul.jpg", location: "Global" },
            { title: "Sean Allen", url: "https://www.youtube.com/@seanallen", description: "Prolific iOS teacher and YouTuber.", location: "Global" },
            { title: "John Sundell", url: "https://www.swiftbysundell.com/", description: "Swift expert and creator of many dev tools.", previewImage: "https://avatars.githubusercontent.com/u/1500155?v=4", location: "Global" },
            { title: "Antoine van der Lee", url: "https://www.avanderlee.com/", description: "iOS Engineer at WeTransfer, Architecture expert.", previewImage: "https://avatars.githubusercontent.com/u/4155106?v=4", location: "Global" },
            { title: "John Gruber", url: "https://daringfireball.net/", description: "Founder of Daring Fireball, Apple commentator.", location: "Global" },
            { title: "Krzysztof Zabłocki", url: "https://www.merowing.info/", description: "Tooling and architecture wizard for iOS.", location: "Global" },
            { title: "Federico Zanetello", url: "https://www.polpietro.com/", description: "iOS expert focusing on Swift internals and API design.", location: "Global" },
            { title: "Majid Jabrayilov", url: "https://twitter.com/mecid", description: "Expert on SwiftUI architecture and modularization.", location: "Global" },
            { title: "Lee Kah Seng", url: "https://twitter.com/Lee_Kah_Seng", description: "Founder of SwiftSenpai and iOS mentor.", location: "Global" }
        ],
        communities: [
            { title: "iOS Developers Slack", url: "https://iosdev.directory/", description: "Large active Slack for iOS and macOS.", location: "Global" },
            { title: "r/iOSProgramming", url: "https://www.reddit.com/r/iOSProgramming/", description: "The primary Reddit community for Apple devs.", location: "Global" },
            { title: "Swift Forums", url: "https://forums.swift.org/", description: "The home of Swift language evolution.", location: "Global" },
            { title: "iOS Dev Directory", url: "https://iosdev.directory/", description: "Comprehensive list of iOS developer blogs and communities.", location: "Global" },
            { title: "Swift Heroes", url: "https://swiftheroes.com/", description: "Global conference and community platform.", location: "Global" }
        ]
    },
    flutter: {
        youtube: [
            { title: "Flutter", url: "https://www.youtube.com/@flutterdev", description: "Official Google channel for all things Flutter.", location: "Global", subscribers: "1.2M" },
            { title: "Reso Coder", url: "https://www.youtube.com/@ResoCoder", description: "Deep dives into clean architecture and DDD.", location: "Global", subscribers: "185k" },
            { title: "FilledStacks", url: "https://www.youtube.com/@filledstacks", description: "Professional-grade Flutter development and architecture.", location: "Global", subscribers: "115k" },
            { title: "Code with Andrea", url: "https://www.youtube.com/@codewithandrea", description: "Expert Flutter tips and architecture from Andrea Bizzotto.", location: "Global", subscribers: "155k" },
            { title: "Flutterly", url: "https://www.youtube.com/@Flutterly", description: "Engaging Flutter tutorials and app walkthroughs.", location: "Global", subscribers: "65k" },
            { title: "Tadas Petra", url: "https://www.youtube.com/@TadasPetra", description: "Rapid Flutter explanations and feature guides.", location: "Global", subscribers: "55k" }
        ].sort((a, b) => {
            const getVal = (s?: string) => s ? (s.includes('M') ? parseFloat(s) * 1000 : parseFloat(s)) : 0;
            return getVal(b.subscribers) - getVal(a.subscribers);
        }),
        courses: [
            { title: "Flutter Boot Camp", url: "https://www.udemy.com/course/flutter-bootcamp-with-dart/", description: "The definitive beginner course by Angela Yu.", location: "Global" },
            { title: "Flutter Foundations", url: "https://codewithandrea.com/courses/flutter-foundations/", description: "Deep architectural dive into the core of Flutter.", location: "Global" },
            { title: "Flutter Apprentice", url: "https://www.kodeco.com/books/flutter-apprentice", description: "The Kodeco book for professional Flutter developers.", location: "Global" },
            { title: "Flutter Foundations Course", url: "https://developer.android.com/courses/flutter-fundamentals", description: "Google's official learning path for Flutter.", location: "Global" }
        ],
        podcasts: [
            { title: "It's All Widgets", url: "https://itsallwidgets.com/podcast", description: "Interviews with top developers in the Flutter world.", location: "Global" },
            { title: "Flying High with Flutter", url: "https://www.flyinghighwithflutter.com/", description: "Technical discussions on Flutter and Dart ecosystem.", location: "Global" },
            { title: "Flutter Tap", url: "https://fluttertap.com/", description: "Weekly insights into the latest Flutter news.", location: "Global" }
        ],
        websites: [
            { title: "Flutter.dev", url: "https://docs.flutter.dev/", description: "Official documentation and source of truth.", location: "Global" },
            { title: "Flutter Community", url: "https://medium.com/flutter", description: "Community-driven technical articles on Medium.", location: "Global" },
            { title: "It's All Widgets Directory", url: "https://itsallwidgets.com/", description: "The definitive directory of Flutter apps.", location: "Global" },
            { title: "Awesome Flutter", url: "https://github.com/Solido/awesome-flutter", description: "Curated list of nearly every useful Flutter library.", location: "Global" }
        ],
        projects: [
            { title: "Riverpod", url: "https://github.com/rrousselGit/riverpod", description: "A reactive caching and state management framework.", location: "Global", stars: "15k" },
            { title: "Provider", url: "https://github.com/rrousselGit/provider", description: "A wrapper around InheritedWidget for state.", location: "Global", stars: "17k" },
            { title: "Flutter Hooks", url: "https://github.com/rrousselGit/flutter_hooks", description: "A Flutter implementation of React Hooks.", location: "Global", stars: "5k" }
        ],
        people: [
            { title: "Andrea Bizzotto", url: "https://codewithandrea.com/", description: "Expert Flutter educator and author.", location: "Global" },
            { title: "Remi Rousselet", url: "https://github.com/rrousselGit", description: "Creator of Riverpod, Provider, and Hooks.", location: "Global" },
            { title: "Eric Seidel", url: "https://twitter.com/eseidel", description: "Co-founder of Flutter and CEO of Shorebird.", location: "Global" }
        ],
        communities: [
            { title: "Flutter Dev Slack", url: "https://flutter.dev/community", description: "Official real-time community chat.", location: "Global" },
            { title: "r/flutterdev", url: "https://www.reddit.com/r/flutterdev/", description: "Primary hub for Flutter developers on Reddit.", location: "Global" }
        ]
    }
};

export const COMMUNITY_RESOURCES: Record<string, CommunityResources> = {
    android: {
        communities: [
            // New Zealand
            { title: "Auckland Android Community", url: "https://www.meetup.com/android-meetup/", description: "Technical talks and social networking.", location: "Auckland", countries: ["NZ"] },
            { title: "GDG Wellington", url: "https://www.meetup.com/gdg-wellington/", description: "Google Developer Group covering Android & Cloud.", location: "Wellington", countries: ["NZ"] },
            { title: "GDG Auckland", url: "https://www.meetup.com/gdg-auckland/", description: "Monthly meetings for Google tech enthusiasts.", location: "Auckland", countries: ["NZ"] },
            { title: "CoderCamp Hamilton", url: "https://www.meetup.com/codercamp-hamilton/", description: "Hamilton's monthly software developer gathering.", location: "Hamilton", countries: ["NZ"] },
            
            // Australia
            { title: "Sydney Android Developers", url: "https://www.meetup.com/sydney-android-developers/", description: "Australia's largest Android community.", location: "Sydney", countries: ["AU"] },
            { title: "Melbourne Android", url: "https://www.meetup.com/melbourne-android-developers/", description: "The home for Android pros in Melbourne.", location: "Melbourne", countries: ["AU"] },
            { title: "GDG Brisbane", url: "https://www.meetup.com/gdg-brisbane/", description: "Google developers in the Sunshine State.", location: "Brisbane", countries: ["AU"] },
            { title: "Perth Android Developers", url: "https://www.meetup.com/perth-android-developers/", description: "Western Australian mobile engineering group.", location: "Perth", countries: ["AU"] },
            
            { title: "CHCH.JS", url: "https://www.meetup.com/chch-js/", description: "JavaScript & Mobile dev community in Christchurch.", location: "Christchurch", countries: ["NZ"] },
            { title: "Waikato Tech", url: "https://www.meetup.com/tech-waikato/", description: "Tech industry group in the Waikato region.", location: "Hamilton", countries: ["NZ"] },
            { title: "Code Craft Dunedin", url: "https://www.meetup.com/code-craft-dunedin/", description: "Dunedin IT development discussion group.", location: "Dunedin", countries: ["NZ"] },
            { title: "Canterbury Tech", url: "https://canterburytech.nz/", description: "The cluster for tech companies in Canterbury.", location: "Christchurch", countries: ["NZ"] },
            { title: "Nelson Dev Group", url: "https://www.meetup.com/nelson-dev/", description: "Tech community in the Nelson region.", location: "Nelson", countries: ["NZ"] },
            { title: "Tauranga Tech", url: "https://www.meetup.com/tauranga-tech/", description: "Growing tech community in the Bay of Plenty.", location: "Tauranga", countries: ["NZ"] }
        ],
        upcomingEvents: [
            { title: "DevFest Auckland 2025", url: "https://devfest.gdgauckland.nz/", description: "The largest Google technology conference in NZ.", location: "Auckland", date: "Nov 9, 2025", countries: ["NZ"] },
            { title: "Droidcon Australia 2025", url: "https://droidcon.com.au/", description: "The premium Android conference in Sydney.", location: "Sydney", date: "Sep 2025", countries: ["AU"] },
            { title: "NZ Tech Rally 2026", url: "https://nztechrally.tech/", description: "Community-driven software conference with mobile tracks.", location: "Wellington", date: "May 15, 2026", countries: ["NZ"] },
            { title: "Techweek NZ 2026", url: "https://techweek.co.nz/", description: "Annual festival of tech and innovation.", location: "NZ-Wide", date: "May 18-24, 2026", countries: ["NZ"] },
            { title: "Canterbury Tech Summit", url: "https://canterburytech.nz/summit/", description: "Premier tech event in the South Island.", location: "Christchurch", date: "Aug 20, 2025", countries: ["NZ"] },
            { title: "Mobile Dev Day NZ", url: "https://devday.io/", description: "Specialized track for mobile development in NZ.", location: "Auckland", date: "Oct 12, 2025", countries: ["NZ"] },
            { title: "AWS Community Day NZ", url: "https://aws-community-day.nz/", description: "Cloud and mobile backend focused event.", location: "NZ-Wide", date: "Sep 5, 2025", countries: ["NZ"] }
        ],
        localProjects: [
            { title: "OkHttp", url: "https://github.com/square/okhttp", description: "The underlying HTTP client for almost all apps.", location: "Global", stars: "47k", countries: ["Global"] },
            { title: "Retrofit", url: "https://github.com/square/retrofit", description: "Industry standard type-safe HTTP client.", location: "Global", stars: "44k", countries: ["Global"] },
            { title: "ExoPlayer", url: "https://github.com/google/ExoPlayer", description: "Powerful media player for Android.", location: "Global", stars: "22k", countries: ["Global"] },
            { title: "Enro", url: "https://github.com/isaac-udy/enro", description: "Powerful navigation library by Isaac Udy.", previewImage: "https://github.com/isaac-udy/enro/raw/main/docs/logo.png", location: "Wellington", stars: "261", countries: ["NZ"] },
            { title: "NZ COVID Pass Verifier", url: "https://github.com/minhealthnz/nzcp-android", description: "Open source verification tool for NZers.", location: "NZ-Wide", stars: "433", countries: ["NZ"] },
            { title: "Open Source NZ", url: "https://github.com/RichardLitt/open-source-aotearoa-nz", description: "List of NZ-based open source projects.", location: "NZ-Wide", stars: "5", countries: ["NZ"] },
            { title: "MadeCurious OSS", url: "https://github.com/madecurious", description: "Projects from Christchurch-based tech hub.", location: "Christchurch", countries: ["NZ"] },
            { title: "Papa Reo", url: "https://papareo.nz/", description: "Open source NLP for Māori language revitalization.", location: "NZ-Wide", countries: ["NZ"] },
            { title: "Personic", url: "https://github.com/personic", description: "Mobile web app for collective digital music creation.", location: "Auckland", countries: ["NZ"] },
            { title: "PaperKite OSS", url: "https://github.com/paperkite", description: "Open source components from Wellington agency.", location: "Wellington", countries: ["NZ"] }
        ],
        localExperts: [
            { title: "Isaac Udy", url: "https://github.com/isaac-udy", description: "Android GDE, Platform Lead at ANZx.", previewImage: "https://avatars.githubusercontent.com/u/18519051?v=4", location: "Wellington", countries: ["NZ"] },
            { title: "Julius Spencer", url: "https://www.linkedin.com/in/juliusspencer/", description: "Founder of GDG Auckland, Director at JSA.", location: "Auckland", countries: ["NZ"] },
            { title: "Sam Hindmarsh", url: "https://hndmr.sh/", description: "GDG Wellington Organizer, Senior Android Engineer.", previewImage: "https://avatars.githubusercontent.com/u/2135075?v=4", location: "Wellington", countries: ["NZ"] },
            { title: "Dilum De Silva", url: "https://github.com/dilumdesilva", description: "GDG Auckland Organizer, Full-stack Engineer.", previewImage: "https://avatars.githubusercontent.com/u/23308182?v=4", location: "Auckland", countries: ["NZ"] },
            { title: "Matt Ranocchiari", url: "https://www.linkedin.com/in/matthewranocchiari/", description: "GDG Auckland & Wellington Organizer.", location: "Auckland/Wellington", countries: ["NZ"] },
            { title: "Rob Prouse", url: "https://github.com/rprouse", description: "CoderCamp Hamilton Organizer and OSS maintainer.", location: "Hamilton", countries: ["NZ"] },
            { title: "Josh Gaber", url: "https://github.com/joshgaber", description: "Hamilton developer and CoderCamp organizer.", location: "Hamilton", countries: ["NZ"] },
            { title: "Tim Penhey", url: "https://github.com/thp", description: "Contributor to Dunedin's tech community.", location: "Dunedin", countries: ["NZ"] },
            { title: "Ian Rees", url: "https://github.com/irees", description: "Code Craft Dunedin facilitator and expert.", location: "Dunedin", countries: ["NZ"] },
            { title: "AJ Bovaird", url: "https://github.com/ajbovaird", description: "Hamilton community organizer and engineer.", location: "Hamilton", countries: ["NZ"] }
        ]
    },
    ios: {
        communities: [
            // New Zealand
            { title: "The Auckland iOS Meetup", url: "https://www.meetup.com/Auckland-iOS-Meetup/", description: "Monthly gatherings for iOS devs.", location: "Auckland", countries: ["NZ"] },
            { title: "CocoaHeads Auckland", url: "https://www.meetup.com/Auckland-CocoaHeads/", description: "Long-running community for Apple devs.", location: "Auckland", countries: ["NZ"] },
            { title: "CocoaHeads Wellington", url: "https://www.meetup.com/Wellington-CocoaHeads/", description: "Wellington based Apple developer group.", location: "Wellington", countries: ["NZ"] },
            
            // Australia
            { title: "Melbourne CocoaHeads", url: "https://www.meetup.com/melbourne-cocoahead/", description: "One of the world's oldest iOS communities.", location: "Melbourne", countries: ["AU"] },
            { title: "Sydney CocoaHeads", url: "https://www.meetup.com/sydney-cocoahead/", description: "Thriving Apple developer group in Sydney.", location: "Sydney", countries: ["AU"] },
            { title: "Brisbane CocoaHeads", url: "https://www.meetup.com/brisbane-cocoahead/", description: "iOS & macOS engineering in Queensland.", location: "Brisbane", countries: ["AU"] },
            { title: "Perth iOS", url: "https://www.meetup.com/perth-ios-developers-meetup-group/", description: "Western Australian Apple platform group.", location: "Perth", countries: ["AU"] },
            
            { title: "Dunedin Gamedev Meetup", url: "https://www.meetup.com/dunedin-gamedev-meetup/", description: "App and game development in Dunedin.", location: "Dunedin", countries: ["NZ"] },
            { title: "Nelson Dev Group", url: "https://www.meetup.com/nelson-dev/", description: "General developer community in Nelson.", location: "Nelson", countries: ["NZ"] },
            { title: "Hamilton Developers", url: "https://techinthetron.com/", description: "Tech industry group in the Tron.", location: "Hamilton", countries: ["NZ"] },
            { title: "Christchurch Game Devs", url: "https://www.meetup.com/christchurch-game-developers/", description: "Game and app workshop group.", location: "Christchurch", countries: ["NZ"] }
        ],
        upcomingEvents: [
            { title: "Dev Day NZ 2025", url: "https://devday.io/", description: "NZ's premier developer conference.", location: "Auckland", date: "Oct 12, 2025", countries: ["NZ"] },
            { title: "Webstock 2025", url: "https://www.webstock.org.nz/", description: "Wellington's iconic tech and design event.", location: "Wellington", date: "May 2025", countries: ["NZ"] },
            { title: "/dev/world 2025", url: "https://devworld.com.au/", description: "The definitive AU conference for Apple devs.", location: "Melbourne", date: "Sep 2025", countries: ["AU"] },
            { title: "Create NZ", url: "https://createnz.io/", description: "Conference focusing on mobile and web craft.", location: "Auckland", date: "July 18, 2025", countries: ["NZ"] },
            { title: "NZGDC 2025", url: "https://nzgdc.com/", description: "NZ Game Developers Conference with mobile tracks.", location: "NZ-Wide", date: "Sep 2025", countries: ["NZ"] }
        ],
        localProjects: [
            { title: "Alamofire", url: "https://github.com/Alamofire/Alamofire", description: "Elegant HTTP networking in Swift.", location: "Global", stars: "43k", countries: ["Global"] },
            { title: "Kickstarter iOS", url: "https://github.com/kickstarter/ios-oss", description: "High-quality functional project reference.", location: "Global", stars: "8.6k", countries: ["Global"] },
            { title: "NZ COVID Tracer OSS", url: "https://github.com/minhealthnz/nz-covid-tracer-app-ios", description: "The official tracker app for New Zealand.", location: "NZ-Wide", stars: "430", countries: ["NZ"] },
            { title: "Open Source NZ", url: "https://github.com/RichardLitt/open-source-aotearoa-nz", description: "List of projects including mobile contributions.", location: "NZ-Wide", stars: "5", countries: ["NZ"] },
            { title: "Swift NZ Tools", url: "https://github.com/Swift-New-Zealand", description: "Community tools and guides for Kiwi Swift devs.", location: "NZ-Wide", countries: ["NZ"] },
            { title: "Alphero Labs", url: "https://github.com/alphero", description: "OSS from Wellington's leading mobile agency.", location: "Wellington", countries: ["NZ"] },
            { title: "DigitalNZ iOS App", url: "https://github.com/DigitalNZ/digitalnz_iphone_app", description: "Official app for accessing NZ's digital heritage.", location: "NZ-Wide", stars: "2", countries: ["NZ"] }
        ],
        localExperts: [
            { title: "Natalia Panferova", url: "https://nilcoalescing.com/", description: "SwiftUI expert, Co-founder of Nil Coalescing.", previewImage: "https://avatars.githubusercontent.com/u/15342922?v=4", location: "Alexandra", countries: ["NZ"] },
            { title: "Dan Too", url: "https://twitter.com/daniel_too", description: "Organizer of Auckland iOS Meetup.", location: "Auckland", countries: ["NZ"] },
            { title: "Stan Bykov", url: "https://www.linkedin.com/in/stanbykov/", description: "Organizer of CocoaHeads Auckland.", location: "Auckland", countries: ["NZ"] },
            { title: "Mitchell Johnson", url: "https://www.linkedin.com/in/mjohnsonnz/", description: "Organizing CocoaHeads Auckland and lead at ANZ.", location: "Auckland", countries: ["NZ"] },
            { title: "Chris Mein", url: "https://github.com/mein", description: "Dunedin tech community facilitator.", location: "Dunedin", countries: ["NZ"] },
            { title: "Xi C.", url: "https://github.com/xic", description: "Prominent iOS/SwiftUI expert based in NZ.", location: "NZ-Wide", countries: ["NZ"] },
            { title: "Jamie W.", url: "https://github.com/jamie-w", description: "Experienced iOS and Flutter dev in NZ.", location: "NZ-Wide", countries: ["NZ"] },
            { title: "Mike Mackenzie", url: "https://github.com/mmackenzie", description: "Code Craft Dunedin organizer and lead engineer.", location: "Dunedin", countries: ["NZ"] }
        ]
    },
    flutter: {
        communities: [
            { title: "Flutter Auckland", url: "https://www.meetup.com/flutter-auckland/", description: "Specialized group for Flutter enthusiasts and pros.", location: "Auckland", countries: ["NZ"] },
            { title: "Flutter Sydney", url: "https://www.meetup.com/flutter-sydney/", description: "Thriving Flutter community in Australia's tech hub.", location: "Sydney", countries: ["AU"] },
            { title: "Flutter Melbourne", url: "https://www.meetup.com/flutter-melbourne/", description: "Melbourne's monthly Flutter developer meetup.", location: "Melbourne", countries: ["AU"] },
            { title: "GDG Auckland", url: "https://www.meetup.com/gdg-auckland/", description: "Broad Google Tech group with heavy Flutter presence.", location: "Auckland", countries: ["NZ"] },
            { title: "GDG Wellington", url: "https://www.meetup.com/gdg-wellington/", description: "Capital city's Google technology community.", location: "Wellington", countries: ["NZ"] },
            { title: "Digital Bytes WGTN", url: "https://www.meetup.com/digital-bytes-wgtn/", description: "Wellington's mobile and web community group.", location: "Wellington", countries: ["NZ"] }
        ],
        upcomingEvents: [
            { title: "Flutter Sydney DevFest", url: "https://devfest.gdgsydney.au/", description: "Major Flutter presence at Sydney's DevFest.", location: "Sydney", date: "Oct 2025", countries: ["AU"] },
            { title: "DevFest Auckland 2025", url: "https://devfest.gdgauckland.nz/", description: "The premier Google technology event in NZ.", location: "Auckland", date: "Nov 9, 2025", countries: ["NZ"] },
            { title: "NZ Tech Rally 2026", url: "https://nztechrally.tech/", description: "Community software conference with Dart/Flutter tracks.", location: "Wellington", date: "May 15, 2026", countries: ["NZ"] },
            { title: "Techweek NZ 2026", url: "https://techweek.co.nz/", description: "National festival of tech across Aotearoa.", location: "NZ-Wide", date: "May 18-24, 2026", countries: ["NZ"] }
        ],
        localProjects: [
            { title: "Riverpod", url: "https://github.com/rrousselGit/riverpod", description: "Reactive caching and state management.", location: "Global", stars: "15k", countries: ["Global"] },
            { title: "Enro", url: "https://github.com/isaac-udy/enro", description: "Navigation library from Wellington dev Isaac Udy.", location: "Wellington", stars: "261", countries: ["NZ"] },
            { title: "Alphero Labs", url: "https://github.com/alphero", description: "OSS experiments from Wellington mobile agency.", location: "Wellington", countries: ["NZ"] },
            { title: "Open Source Aotearoa", url: "https://github.com/RichardLitt/open-source-aotearoa-nz", description: "Curated list of Kiwi open source work.", location: "NZ-Wide", stars: "5", countries: ["NZ"] }
        ],
        localExperts: [
            { title: "Dilum De Silva", url: "https://github.com/dilumdesilva", description: "GDG Auckland Organizer and Flutter GDE.", location: "Auckland", countries: ["NZ"] },
            { title: "Julius Spencer", url: "https://www.linkedin.com/in/juliusspencer/", description: "Founder of GDG Auckland, Flutter/Android expert.", location: "Auckland", countries: ["NZ"] },
        ]
    }
};
