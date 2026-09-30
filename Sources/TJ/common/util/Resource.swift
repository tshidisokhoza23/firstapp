import Foundation

public enum Resource<T>: Equatable where T: Equatable {
    case success(T)
    case error(String, T? = nil)
    case loading(T? = nil)

    public var data: T? {
        switch self {
        case .success(let value):
            return value
        case .error(_, let lastData):
            return lastData
        case .loading(let partialData):
            return partialData
        }
    }

    public var message: String? {
        switch self {
        case .error(let message, _):
            return message
        default:
            return nil
        }
    }

    public var isLoading: Bool {
        if case .loading = self { return true }
        return false
    }

    public var isSuccess: Bool {
        if case .success = self { return true }
        return false
    }

    public var isError: Bool {
        if case .error = self { return true }
        return false
    }
}
