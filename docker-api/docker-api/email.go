package main

import (
	log "github.com/sirupsen/logrus"
	"gopkg.in/gomail.v2"
)

func sendEmail(subject string, body string) error {
	m := gomail.NewMessage()
	m.SetHeader("From", c.EmailFrom)
	m.SetHeader("To", c.EmailTo...)
	m.SetHeader("Subject", subject)
	m.SetBody("text/plain", body)

	var d *gomail.Dialer
	if c.EmailUsername != "" && c.EmailPassword != "" {
		d = gomail.NewDialer(c.EmailServer, c.EmailPort, c.EmailUsername, c.EmailPassword)
	} else {
		d = &gomail.Dialer{Host: c.EmailServer, Port: c.EmailPort}
	}

	if err := d.DialAndSend(m); err != nil {
		log.Errorf("Error sending email : %s", err)
		return err
	}
	log.Infof("Email Sent Successfully!")
	return nil
}
