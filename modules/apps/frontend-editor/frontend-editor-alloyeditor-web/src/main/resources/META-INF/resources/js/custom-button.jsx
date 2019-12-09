import React from 'react';
 import {AlloyEditor} from 'alloyeditor';

 class CustomButton extends React.Component {
    static defaultProps = {
        command: 'googledocsselector'
    };

    static key = 'googledocs';

    render() {
        return (
            <button className="ae-button" data-type="button-googledocs" onClick={this.execCommand} tabIndex={this.props.tabIndex}>
                <span className="icon-file">?</span>
            </button>
        );
    }
} 

 const customButton = AlloyEditor.Base.ButtonCommand(CustomButton);
 AlloyEditor.Buttons.customButton = customButton;

 export default customButton;