import React, { Component } from 'react';
import { AppRegistry, View } from 'react-native';
import Routes from './Routes.js'

class VILABLE_APP extends Component {
   constructor () {
      super();
      console.disableYellowBox = true;
   }
   render() {
      return (
         <Routes />
      )
   }
}
export default VILABLE_APP
AppRegistry.registerComponent('VILABLE_APP', () => VILABLE_APP)