import 'dart:io';

void main() {
  final examplePubspecs = Directory.current
      .listSync(recursive: true)
      .whereType<File>()
      .where((e) => e.path.endsWith('example/pubspec.yaml'));

  for (final file in examplePubspecs) {
    final split = file.parent.path.split(Platform.pathSeparator);
    final packageName = split[split.length - 2];
    file.writeAsStringSync(file
        .readAsStringSync()
        .replaceFirst(RegExp('name: .*'), 'name: ${packageName}_example'));
  }
}
