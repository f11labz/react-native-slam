import React, { Component } from 'react';
import {
  StyleSheet,
  View,
  ScrollView,
  AppState,
  Dimensions,
  SafeAreaView,
  TouchableOpacity,
} from 'react-native';
import ModalDropdown from 'react-native-modal-dropdown';
import { Button } from 'react-native-elements';
import Svg, {
  Polygon,
  Circle, Text as SVGText
} from 'react-native-svg';
import { RNCamera } from 'react-native-camera';
import ImgToBase64 from 'react-native-image-base64';
import ImageResizer from 'react-native-image-resizer';
global.fetch = require('node-fetch')
const window = Dimensions.get('window');
const viewPort = {
  width: window.width <= window.height ? window.width - 10 : window.height - 10,
  height: window.width <= window.height ? window.width - 10 : window.height - 10
}
var selectedOption = '';

import OpencvModule from './modules/opencv/index';

export default class Appin extends Component {
  constructor() {
    super();
    this.state = {
      appState: '',
      base64Image: '',
      shotFlag: 1,
      collapsed: false,
      rotationOptions: [-90, 0, +90],
      cameraRotation: 0
    }
    this.translation_state = '';
    this.slamData = {};
    this.slamFrames = 0;
    this.lastSlamPath = [];
    this.slamPath = [];
    this.slamBlockedCells = {};
    this.slamLastBlockedCells = {
      count: 0,
      cells: [],
      isLastSpeaked: true
    };
    this.slamLastProcessTime = 0;
  }

  async componentDidMount() {
    console.disableYellowBox = true;
    AppState.addEventListener('change', this.handleAppStateChange);
    selectedOption = 'walkingAssistanceSlam';
  }


  setImage(base64Image) {
    this.setState({ base64Image: base64Image });
  }

  async updateSlam(image) {
    console.log('slam start ', new Date().toISOString())
    OpencvModule.detectPoints(image, (err, points) => {
      console.log('slam end ', new Date().toISOString())
      if (err) {
        console.log('opencv err ', err);
        return;
      }
      this.slamData = {
        coordinates: points
      }
    });
  }

  handleAppStateChange(nextAppState) {
    if (this.state.appState.match(/inactive|background/) && nextAppState === 'active') {
      console.log('App has come to the foreground!')
    }
    this.setState({ appState: nextAppState });
  }

  componentWillUnmount() {

  }

  classifyImageTfLite_v1_slam(base64Image = '') {
    let log_id = new Date().toISOString();
    console.log('Object detection starting for log_id', log_id, new Date().toISOString());
    this.updateSlam(base64Image);
  }

  getPictures() {
    this.payload_distance = "1 meter";
    console.log("resuming camera preview");
    this.camInterval = setInterval(this.takePicture, 1000);
  }

  stopPictures() {
    clearInterval(this.camInterval);
  }

  takePicture = async () => {
    if (this.camera) {
      const options = { quality: 0.5, base64: true, orientation: RNCamera.Constants.Orientation.portrait };
      const data = await this.camera.takePictureAsync(options);
      console.log('pictureOrientation', data.pictureOrientation, 'deviceOrientation', data.deviceOrientation, data.exif, Number(this.state.cameraRotation));
      let imageuri = 'data:image/jpeg;base64,' + data.base64;
      ImageResizer.createResizedImage(imageuri, 600, 600, 'JPEG', 100, Number(this.state.cameraRotation))
        .then(response => {
          ImgToBase64.getBase64String(response.uri).then(base64String => {
            this.performAction(base64String);
          }).catch(errRead => {
            console.log(errRead);
          })
        })
        .catch(err => {
          console.log(err);
        });
    }
  };

  async performAction(imageData) {
    console.log('performing action for ', selectedOption, new Date().toISOString());
    if (selectedOption) {
      switch (selectedOption) {
        case 'walkingAssistanceSlam':
          this.setImage(imageData);
          this.classifyImageTfLite_v1_slam(imageData);
          console.log('complete action for ', selectedOption, new Date().toISOString());
          break;
        default:
          console.log('Invalid option selected');
      }
    } else {
      console.log('No option selected');
    }
  }

  setArrowImage(isCollapsed) {
    this.setState({ collapsed: isCollapsed });
  }

  drawSlam() {
    let points = [];
    // console.log(this.slamData.coordinates);
    if (this.slamData.coordinates && this.slamData.coordinates.length > 0) {
      // let cords = JSON.parse(this.slamData.coordinates);
      let cords = this.slamData.coordinates;
      // console.log(cords);
      for (let i = 0; i < cords.length; i++) {
        let rm = i % 10
        if (rm) continue;
        let p = cords[i];
        let x = p[0] * viewPort.width / 600;
        let y = p[1] * viewPort.height / 600
        points.push(
          <Circle cx={x} cy={y} r="2" fill="green" />
        )
      }
    }
    return points
  }
  drawPolygon() {
    let points = [];
    // console.log(this.slamData.coordinates);
    if (this.state.wall && this.state.wall.length > 0) {
      let cords = this.state.wall;
      for (let cord of cords) {
        let vCords = [];
        for (let p of cord) {
          let x = p[0] * viewPort.width / 600;
          let y = p[1] * viewPort.height / 600;
          vCords.push([x, y]);
        }
        console.log('drawing polygon for', vCords);
        points.push(
          <Polygon
            points={vCords.join(", ")}
            stroke="green"
            strokeWidth="2"
          />
        )
      }
    }
    return points
  }

  drawPointsTexts() {
    let textPos = {
      l1: [viewPort.width * .10, viewPort.height * .90],
      l2: [viewPort.width * .20, viewPort.height * .70],
      l3: [viewPort.width * .30, viewPort.height * .60],
      c1: [viewPort.width / 2, viewPort.height * .90],
      c2: [viewPort.width / 2, viewPort.height * .70],
      c3: [viewPort.width / 2, viewPort.height * .60],
      r1: [viewPort.width * .85, viewPort.height * .90],
      r2: [viewPort.width * .77, viewPort.height * .70],
      r3: [viewPort.width * .70, viewPort.height * .60]
    }
    let texts = [];
    if (this.slamBlockedCells) {
      for (let i in this.slamBlockedCells) {
        texts.push(
          <SVGText fontSize="20" fontWeight="bold" x={textPos[i][0] + ""} y={textPos[i][1] + ""} fill="red">{this.slamBlockedCells[i]}</SVGText>
        )
      }
    }
    return texts
  }
  setDropdown(data) {
    data.left = 10
    return data;
  }

  render() {
    const base64Image = this.state.base64Image;
    return (
      <SafeAreaView style={styles.container}>
        <View style={styles.container}>
          <View style={styles.container1}>
            <TouchableOpacity style={
              [styles.imageContainer, {
                borderWidth: base64Image ? 0 : 1
              }]}
            >
              <View>
                < RNCamera
                  style={[styles.preview, {
                    height: 100,
                    width: 100
                  }]}
                  ref={ref => { this.camera = ref; }}
                  type={RNCamera.Constants.Type.back}
                  flashMode={RNCamera.Constants.FlashMode.off}
                  cameraViewDimensions={{ width: 100, height: 100 }}
                  ratio="1:1"
                  // onPictureTaken={d => {this.onPictureTakenEv(d)}}
                  androidCameraPermissionOptions={{
                    title: 'Permission to use camera',
                    message: 'We need your permission to use your camera',
                    buttonPositive: 'Ok',
                    buttonNegative: 'Cancel',
                  }}
                  androidRecordAudioPermissionOptions={{
                    title: 'Permission to use audio recording',
                    message: 'We need your permission to use your audio',
                    buttonPositive: 'Ok',
                    buttonNegative: 'Cancel',
                  }}
                  playSoundOnCapture={false}
                >
                </RNCamera>
              </View>
              <View style={styles.boxes}>
                <Svg width={viewPort.width} height={viewPort.height}>
                  {
                    this.drawSlam()
                  }
                  {
                    // this.drawPointsTexts()
                  }
                </Svg>
              </View>
            </TouchableOpacity>
          </View>
          <ScrollView style={styles.scroll}>
            <View style={styles.collapsibleBody}>
              <View style={{ margin: 5 }}>
                <Button id="idmodebtn" title="Start Feed" onPress={() => this.getPictures()} />
              </View>

              <View style={styles.flexBtn}>
                <Button title="Stop Feed" onPress={() => this.stopPictures()} />
              </View>
              <View>
                <ModalDropdown
                  defaultValue="Camera Rotation"
                  options={this.state.rotationOptions}
                  style={styles.sectionStyleDrpdwn}
                  ref="cameraRotation"
                  dropdownStyle={{ height: 100, width: '90%', marginLeft: 10, marginRight: 10 }}
                  onSelect={(value) => this.setState({ cameraRotation: this.state.rotationOptions[value] })}
                  adjustFrame={(data) => this.setDropdown(data)}
                />
              </View>
            </View>
          </ScrollView>
        </View>
      </SafeAreaView>
    );
  }
}

module.export = Appin

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#FFF',
    width: window.width,
    height: window.height
  },
  scroll: {
    flex: 1,
    backgroundColor: '#f0f0f0',
    margin: 10,
  },
  row: {
    margin: 10
  },
  container1: {
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'white'
  },
  imageContainer: {
    borderColor: 'blue',
    borderRadius: 5,
    alignItems: "center",
    width: viewPort.width,
    height: viewPort.width
  },
  text: {
    color: 'blue'
  },
  button: {
    width: 200,
    backgroundColor: 'blue',
    borderRadius: 10,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 10
  },
  buttonText: {
    color: 'white',
    fontSize: 15
  },
  box: {
    position: 'absolute',
    borderColor: 'blue',
    borderWidth: 2,
  },
  boxes: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    top: 0,
  },
  boxes2: {
    position: 'absolute',
    borderColor: 'green',
    borderWidth: 2,
    left: 30,
    right: 30,
    bottom: 30,
    top: 30,
  },
  preview: {
    flex: 1,
    justifyContent: 'flex-end',
    alignItems: 'center',
  },
  capture: {
    flex: 0,
    backgroundColor: '#fff',
    borderRadius: 5,
    padding: 15,
    zIndex: 999,
    paddingHorizontal: 20,
    alignSelf: 'center',
    margin: 20,
  },
  flexContainer: {
    display: 'flex',
    flexWrap: 'nowrap',
    overflow: 'scroll',
    flexDirection: 'row',
    height: 40,
    marginBottom: 5
  },
  flexBtn: {
    width: 'auto',
    margin: 5
  },
  imgStyle: {
    padding: 5,
    marginLeft: 10
  },
  arrowImgStyle: {
    margin: 8,
    height: 15,
    width: 15,
    resizeMode: 'contain',
    alignItems: 'flex-start'
  },
  collapsible: {
    padding: 10,
    backgroundColor: '#E6E6E6'
  },
  collapsibleText: {
    fontSize: 16,
    marginLeft: 8,
    color: 'black',
    width: 200
  },
  cheader: {
    flexDirection: 'row',
    width: '50%',
  },
  cameraContainer: {
    height: 100,
    width: 100,
    position: 'absolute',
    top: 0,
    right: 0,
    backgroundColor: 'red',
    zIndex: 9
  },
  collapsibleBody: {
    borderColor: 'lightgray',
    borderWidth: 1,
    padding: 5
  },
  sectionStyleDrpdwn: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#fff',
    borderWidth: 0.5,
    borderColor: '#000',
    height: 40,
    borderRadius: 5,
    margin: 10,
    marginLeft: '3%',
    width: '94%'
  },
});
