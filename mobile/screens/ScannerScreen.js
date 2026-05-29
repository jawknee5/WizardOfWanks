import React, { useState, useRef } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Alert, ActivityIndicator } from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import * as FileSystem from 'expo-file-system';
import { Camera } from 'expo-camera';

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    padding: 20,
    justifyContent: 'center',
  },
  button: {
    backgroundColor: '#667eea',
    padding: 16,
    borderRadius: 8,
    marginVertical: 10,
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
    textAlign: 'center',
  },
  loading: {
    marginTop: 20,
    justifyContent: 'center',
    alignItems: 'center',
  },
});

export default function ScannerScreen({ navigation }) {
  const [loading, setLoading] = useState(false);
  const cameraRef = useRef(null);

  const requestCameraPermission = async () => {
    const { status } = await Camera.requestCameraPermissionsAsync();
    return status === 'granted';
  };

  const requestPhotoPermission = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    return status === 'granted';
  };

  const takePhoto = async () => {
    const hasPermission = await requestCameraPermission();
    if (!hasPermission) {
      Alert.alert('Permission Required', 'Camera permission is required');
      return;
    }

    try {
      setLoading(true);
      const result = await ImagePicker.launchCameraAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsEditing: false,
        aspect: [4, 3],
        quality: 1,
      });

      if (!result.canceled) {
        const imageUri = result.assets[0].uri;
        // Process image with OCR
        navigation.navigate('Editor', { imageUri });
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to take photo: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  const selectFromGallery = async () => {
    const hasPermission = await requestPhotoPermission();
    if (!hasPermission) {
      Alert.alert('Permission Required', 'Photo library permission is required');
      return;
    }

    try {
      setLoading(true);
      const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsEditing: false,
        aspect: [4, 3],
        quality: 1,
      });

      if (!result.canceled) {
        const imageUri = result.assets[0].uri;
        navigation.navigate('Editor', { imageUri });
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to select photo: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>📸 Scan Receipt</Text>

      <TouchableOpacity style={styles.button} onPress={takePhoto} disabled={loading}>
        <Text style={styles.buttonText}>📷 Take Photo</Text>
      </TouchableOpacity>

      <TouchableOpacity style={styles.button} onPress={selectFromGallery} disabled={loading}>
        <Text style={styles.buttonText}>🖼️ Select from Gallery</Text>
      </TouchableOpacity>

      {loading && (
        <View style={styles.loading}>
          <ActivityIndicator size="large" color="#667eea" />
          <Text style={{ marginTop: 10 }}>Processing receipt...</Text>
        </View>
      )}
    </View>
  );
}
