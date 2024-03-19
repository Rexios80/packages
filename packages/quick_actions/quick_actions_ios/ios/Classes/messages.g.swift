// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
// Autogenerated from Pigeon (v12.0.1), do not edit directly.
// See also: https://pub.dev/packages/pigeon

import Foundation

#if os(iOS)
  import Flutter
#elseif os(macOS)
  import FlutterMacOS
#else
  #error("Unsupported platform.")
#endif

extension FlutterError: Error {}

private func isNullish(_ value: Any?) -> Bool {
  return value is NSNull || value == nil
}

private func wrapResult(_ result: Any?) -> [Any?] {
  return [result]
}

private func wrapError(_ error: Any) -> [Any?] {
  if let flutterError = error as? FlutterError {
    return [
      flutterError.code,
      flutterError.message,
      flutterError.details,
    ]
  }
  return [
    "\(error)",
    "\(type(of: error))",
    "Stacktrace: \(Thread.callStackSymbols)",
  ]
}

private func nilOrValue<T>(_ value: Any?) -> T? {
  if value is NSNull { return nil }
  return value as! T?
}

/// Home screen quick-action shortcut item.
///
/// Generated class from Pigeon that represents data sent in messages.
struct ShortcutItemMessage {
  /// The identifier of this item; should be unique within the app.
  var type: String
  /// Localized title of the item.
  var localizedTitle: String
  /// Name of native resource to be displayed as the icon for this item.
  var icon: String? = nil

  static func fromList(_ list: [Any?]) -> ShortcutItemMessage? {
    let type = list[0] as! String
    let localizedTitle = list[1] as! String
    let icon: String? = nilOrValue(list[2])

    return ShortcutItemMessage(
      type: type,
      localizedTitle: localizedTitle,
      icon: icon
    )
  }
  func toList() -> [Any?] {
    return [
      type,
      localizedTitle,
      icon,
    ]
  }
}
private class IOSQuickActionsApiCodecReader: FlutterStandardReader {
  override func readValue(ofType type: UInt8) -> Any? {
    switch type {
    case 128:
      return ShortcutItemMessage.fromList(self.readValue() as! [Any?])
    default:
      return super.readValue(ofType: type)
    }
  }
}

private class IOSQuickActionsApiCodecWriter: FlutterStandardWriter {
  override func writeValue(_ value: Any) {
    if let value = value as? ShortcutItemMessage {
      super.writeByte(128)
      super.writeValue(value.toList())
    } else {
      super.writeValue(value)
    }
  }
}

private class IOSQuickActionsApiCodecReaderWriter: FlutterStandardReaderWriter {
  override func reader(with data: Data) -> FlutterStandardReader {
    return IOSQuickActionsApiCodecReader(data: data)
  }

  override func writer(with data: NSMutableData) -> FlutterStandardWriter {
    return IOSQuickActionsApiCodecWriter(data: data)
  }
}

class IOSQuickActionsApiCodec: FlutterStandardMessageCodec {
  static let shared = IOSQuickActionsApiCodec(readerWriter: IOSQuickActionsApiCodecReaderWriter())
}

/// Generated protocol from Pigeon that represents a handler of messages from Flutter.
protocol IOSQuickActionsApi {
  /// Sets the dynamic shortcuts for the app.
  func setShortcutItems(itemsList: [ShortcutItemMessage]) throws
  /// Removes all dynamic shortcuts.
  func clearShortcutItems() throws
}

/// Generated setup class from Pigeon to handle messages through the `binaryMessenger`.
class IOSQuickActionsApiSetup {
  /// The codec used by IOSQuickActionsApi.
  static var codec: FlutterStandardMessageCodec { IOSQuickActionsApiCodec.shared }
  /// Sets up an instance of `IOSQuickActionsApi` to handle messages through the `binaryMessenger`.
  static func setUp(binaryMessenger: FlutterBinaryMessenger, api: IOSQuickActionsApi?) {
    /// Sets the dynamic shortcuts for the app.
    let setShortcutItemsChannel = FlutterBasicMessageChannel(
      name: "dev.flutter.pigeon.quick_actions_ios.IOSQuickActionsApi.setShortcutItems",
      binaryMessenger: binaryMessenger, codec: codec)
    if let api = api {
      setShortcutItemsChannel.setMessageHandler { message, reply in
        let args = message as! [Any?]
        let itemsListArg = args[0] as! [ShortcutItemMessage]
        do {
          try api.setShortcutItems(itemsList: itemsListArg)
          reply(wrapResult(nil))
        } catch {
          reply(wrapError(error))
        }
      }
    } else {
      setShortcutItemsChannel.setMessageHandler(nil)
    }
    /// Removes all dynamic shortcuts.
    let clearShortcutItemsChannel = FlutterBasicMessageChannel(
      name: "dev.flutter.pigeon.quick_actions_ios.IOSQuickActionsApi.clearShortcutItems",
      binaryMessenger: binaryMessenger, codec: codec)
    if let api = api {
      clearShortcutItemsChannel.setMessageHandler { _, reply in
        do {
          try api.clearShortcutItems()
          reply(wrapResult(nil))
        } catch {
          reply(wrapError(error))
        }
      }
    } else {
      clearShortcutItemsChannel.setMessageHandler(nil)
    }
  }
}
/// Generated protocol from Pigeon that represents Flutter messages that can be called from Swift.
protocol IOSQuickActionsFlutterApiProtocol {
  /// Sends a string representing a shortcut from the native platform to the app.
  func launchAction(
    action actionArg: String, completion: @escaping (Result<Void, FlutterError>) -> Void)
}
class IOSQuickActionsFlutterApi: IOSQuickActionsFlutterApiProtocol {
  private let binaryMessenger: FlutterBinaryMessenger
  init(binaryMessenger: FlutterBinaryMessenger) {
    self.binaryMessenger = binaryMessenger
  }
  /// Sends a string representing a shortcut from the native platform to the app.
  func launchAction(
    action actionArg: String, completion: @escaping (Result<Void, FlutterError>) -> Void
  ) {
    let channel = FlutterBasicMessageChannel(
      name: "dev.flutter.pigeon.quick_actions_ios.IOSQuickActionsFlutterApi.launchAction",
      binaryMessenger: binaryMessenger)
    channel.sendMessage([actionArg] as [Any?]) { _ in
      completion(.success(Void()))
    }
  }
}
