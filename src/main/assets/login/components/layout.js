import React from 'react'
import {Button, Divider, Grid, Header, Icon, Segment} from 'semantic-ui-react'

export default class Layout extends React.Component {

  render() {
    return (
        <Grid textAlign='center' verticalAlign='middle'>
          <Grid.Column>
            <Segment>
              <Header as='h2' color='grey' textAlign='center'>Log in</Header>
              <Button.Group vertical fluid size='large'>
                <Button color='teal'>Login with Password</Button>
                <Button color='facebook'><Icon name='facebook'/>Login with Facebook</Button>
                <Button color='google plus'><Icon name='google plus'/>Login with Google</Button>
                <Button color='instagram'>Login with ID-Card</Button>
              </Button.Group>
              <Divider hidden/>
              <Button circular icon='settings' size='large'/>
              <Button circular icon='settings' size='large'/>
            </Segment>
          </Grid.Column>
        </Grid>
    )
  }
}