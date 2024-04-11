import 'dart:io';

void main() {
  final examplePubspecs = Directory.current
      .listSync(recursive: true, followLinks: false)
      .whereType<File>()
      .where((e) => e.path.contains(RegExp(r'\/example.*pubspec\.yaml')));

  for (final file in examplePubspecs) {
    if (file.path.contains('maps_example_dart')) continue;
    final split = file.parent.path.split(Platform.pathSeparator);
    final exampleFolderIndex = split.indexWhere((e) => e == 'example');
    final packageName = split.skip(exampleFolderIndex - 1).join('_');
    file.writeAsStringSync(file
        .readAsStringSync()
        .replaceFirst(RegExp('name: .*'), 'name: $packageName'));
  }
}
