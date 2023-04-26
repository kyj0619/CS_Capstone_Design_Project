import React, {useState} from "react";
import classes from "./Inputpin.module.css";
import head from "../images/head.png";



const Inputpin = (props) => {

    const [pinValue, setPinValue] = useState("");

    const pinChangeHandler = (event) => {
        setPinValue(event.target.value);
        console.log(pinValue)
    }

    return (
        <div className={classes.pindiv}>
            <img src={head} alt="Park"/>
            <input className={classes.pin} type="text" onChange={pinChangeHandler} />
            <div className={classes.textzone}>
                pin 잊음
            </div>
            <button onClick={props.fullchange}>wow!</button>
        </div>
    );
};

export default Inputpin;