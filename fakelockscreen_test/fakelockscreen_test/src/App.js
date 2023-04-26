import React, {useEffect, useState} from 'react';
import Inputpin from './components/Inputpin';
import classes from './App.module.css';


function App() {

  useEffect(() => {
    const elem = document.documentElement;
    setTimeout(() => {
      if (elem.requestFullscreen) {
        elem.requestFullscreen();
      } else if (elem.webkitRequestFullscreen) {
        elem.webkitRequestFullscreen();
      }
      console.log('fullscreen')
    }, 2000);
  }, []);

  const [fulled, setfulled] = useState(false);

  const changescreenhandler = () => {
    if(fulled === false){
      setfulled(true);
    }else{
      setfulled(false);
    }
  }

  
  return (
    <div className={classes.fullscreen}>
      <Inputpin fullchange={changescreenhandler} />
    </div>
  );
}

export default App;
