import Foundation

public enum Resource<T: Equatable>: Equatable {
    case loading(T? = nil)
    case success(T)
    case error(String, T? = nil)

    public var data: T? {
        switch self {
        case .loading(let data), .error(_, let data):
            return data
        case .success(let data):
            return data
        }
    }
}
