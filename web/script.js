function calculateHash(event) {
    event.preventDefault()

    let formData = new FormData(submitForm)
    for ([key, value] of formData.entries()) {
        if (value instanceof File) {
            console.log(value.name);

            let fileReader = new FileReader()
            fileReader.readAsBinaryString(value)
            fileReader.onloadend = function () {
                console.log(CryptoJS.MD5(CryptoJS.enc.Latin1.parse(fileReader.result)).toString())
            }

        }
    }
}

const submitForm = document.getElementById("submit_form")
submitForm.addEventListener("submit", calculateHash)