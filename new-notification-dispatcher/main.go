package main

import (
	"fmt"
	"net/http"
)

func main() {
	server := &http.Server{
		Addr:    ":3000",
		Handler: http.HandlerFunc(handler),
	}

	err := server.ListenAndServe()
	if err != nil {
		fmt.Println("ERROR!")
	}
}

func handler(w http.ResponseWriter, r *http.Request) {
	w.Write([]byte("New-notification-dispatcher"))
}
