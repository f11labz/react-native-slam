import React from 'react'
import { Router, Scene } from 'react-native-router-flux'
import Appin from './Appin.js'

const Routes = () => (
   <Router>
      <Scene key = "root">
         <Scene key = "Appin" component = {Appin} title = "V I L A"   initial = {true} />
      </Scene>
   </Router>
)
export default Routes