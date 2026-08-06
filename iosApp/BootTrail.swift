import Foundation

/// Swift side of the startup breadcrumb trail. Shares the same UserDefaults keys as the
/// Kotlin `BootTrail` object so stages recorded before the KMP framework starts are visible too.
enum BootTrailSwift {
    static let currentKey = "anytv.boot_trail"
    static let previousKey = "anytv.prev_boot_trail"
    static let crashKey = "anytv.last_crash"

    /// Moves the trail of the launch that just ended into the "previous" slot and starts a fresh one.
    static func rotate() {
        let defaults = UserDefaults.standard
        let finished = defaults.string(forKey: currentKey) ?? ""
        defaults.set(finished, forKey: previousKey)
        defaults.set("", forKey: currentKey)
        defaults.synchronize()
    }

    static func mark(_ stage: String) {
        let defaults = UserDefaults.standard
        let current = defaults.string(forKey: currentKey) ?? ""
        let updated = current.isEmpty ? stage : current + "\n" + stage
        defaults.set(updated, forKey: currentKey)
        defaults.synchronize()
    }

    static func recordCrash(_ details: String) {
        let defaults = UserDefaults.standard
        defaults.set(details, forKey: crashKey)
        defaults.synchronize()
    }

    /// Catches hard crashes that never reach the Kotlin unhandled exception hook.
    static func installSignalHandlers() {
        let handler: @convention(c) (Int32) -> Void = { signalNumber in
            let name: String
            switch signalNumber {
            case SIGSEGV: name = "SIGSEGV (invalid memory access)"
            case SIGABRT: name = "SIGABRT (abort)"
            case SIGBUS: name = "SIGBUS (bus error)"
            case SIGILL: name = "SIGILL (illegal instruction)"
            case SIGFPE: name = "SIGFPE (arithmetic error)"
            case SIGTRAP: name = "SIGTRAP (trap)"
            case SIGPIPE: name = "SIGPIPE (broken pipe)"
            default: name = "signal \(signalNumber)"
            }
            let symbols = Thread.callStackSymbols.joined(separator: "\n")
            BootTrailSwift.recordCrash("FATAL \(name)\n\n\(symbols)")
            signal(signalNumber, SIG_DFL)
            raise(signalNumber)
        }

        for sig in [SIGSEGV, SIGABRT, SIGBUS, SIGILL, SIGFPE, SIGTRAP, SIGPIPE] {
            signal(sig, handler)
        }
    }

    static func installExceptionHandler() {
        NSSetUncaughtExceptionHandler { exception in
            let details = """
            ObjC \(exception.name.rawValue): \(exception.reason ?? "no reason")

            \(exception.callStackSymbols.joined(separator: "\n"))
            """
            BootTrailSwift.recordCrash(details)
        }
    }
}
